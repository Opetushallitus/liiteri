(ns liiteri.virus-scan
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.files.file-store :as file-store]
            [taoensso.timbre :as log]
            [cheshire.core :as json])
  (:import [com.amazonaws.services.sqs.model ReceiveMessageRequest]))

(defn- log-virus-scan-result [file-key filename content-type status elapsed-time]
  (let [status-str (string/upper-case (name status))]
    (log/info (str "Virus scan took " elapsed-time " ms, status " status-str " for file " filename " with key " file-key " (" content-type ")"))))

(defn- mark-and-log-failure [file-key filename content-type max-retry-count retry-wait-minutes conn]
  (let [status (metadata-store/mark-virus-scan-for-retry-or-fail file-key max-retry-count retry-wait-minutes conn)]
    (log/warn (str "Failed to scan file " filename " with key " file-key ": " (:virus-scan-status status) ", retry " (:virus-scan-retry-count status) " of " max-retry-count))
    (when (= (:virus-scan-status status) "failed")
      (log/error "FINAL: Scan of file " filename " with key " file-key " (" content-type ") will not be retried"))))

(defn- poll-scan-results [sqs-client result-queue-url db storage-engine config]
  (try
     (let [messages (-> (.receiveMessage sqs-client (-> (ReceiveMessageRequest. result-queue-url)
                                                        (.withWaitTimeSeconds (int 1)))) ; wait time of 1 second is to enable long polling which means we get answers from all sqs servers
                        (.getMessages))]
       (doseq [message messages]
         (try
           (let [message (json/parse-string (.getBody message) true)
                 scan-result (json/parse-string (:Message message) true)]
             (when [= (:bucket scan-result) (get-in config [:file-store :s3 :bucket])]
               (let [file-key (:key scan-result)
                     custom-data (json/parse-string (:custom_data scan-result "{}") true)
                     start-time (:start-time custom-data)
                     elapsed-time (if start-time (- (System/currentTimeMillis) start-time) nil)
                     filename (:filename custom-data)
                     content-type (:content-type custom-data)]
                 (jdbc/with-db-transaction [tx db]
                                           (let [conn {:connection tx}]
                                             (case (:status scan-result)
                                               "clean" (do
                                                         (log-virus-scan-result file-key filename content-type :ok elapsed-time)
                                                         (metadata-store/set-virus-scan-status! file-key "done" conn))
                                               "infected" (do
                                                            (log-virus-scan-result file-key filename content-type :virus-found elapsed-time)
                                                            (file-store/delete-file-and-metadata file-key "liiteri-virus-scan" storage-engine conn false)
                                                            (metadata-store/set-virus-scan-status! file-key "virus_found" conn))
                                               (mark-and-log-failure file-key filename content-type 0 0 conn)))))))
           (.deleteMessage sqs-client result-queue-url (.getReceiptHandle message))
           (catch Exception e
             (log/error e (str "Failed to process scan result for message: " (.getBody message))))))
       (.size messages))
     (catch Exception e
       (log/error e "Failed to process messages from scan result queue")
       0)))

(defprotocol Scanner
  (request-file-scan [this file-key filename content-type]))

(defrecord VirusScanner [db storage-engine config sqs-client]
  component/Lifecycle

  (start [this]
    (let [request-queue-name (get-in config [:bucketav :scan-request-queue-name])
          request-queue-url (-> (.getQueueUrl (:sqs-client sqs-client) request-queue-name)
                                (.getQueueUrl))
          result-queue-name (get-in config [:bucketav :scan-result-queue-name])
          result-queue-url (-> (.getQueueUrl (:sqs-client sqs-client) result-queue-name)
                                (.getQueueUrl))
          poll-interval (get-in config [:bucketav :poll-interval-seconds])
          times         (c/chime-ch (p/periodic-seq (t/now) (t/seconds poll-interval))
                                    {:ch (a/chan (a/sliding-buffer 1))})
          s3-bucket (get-in config [:file-store :s3 :bucket])]
      (log/info "Starting virus scan results polling")
      (a/go-loop []
        (when-let [_ (a/<! times)]
          (while (< 0 (poll-scan-results (:sqs-client sqs-client) result-queue-url db storage-engine config)))
          (recur)))
      (assoc this :chan times
                  :request-queue-url request-queue-url
                  :sqs-client (:sqs-client sqs-client)
                  :s3-bucket s3-bucket)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan))
    (log/info "Stopped virus scan results polling")
    (assoc this :chan nil
                :request-queue-url nil
                :sqs-client nil
                :s3-bucket nil))

  Scanner

  (request-file-scan [this file-key filename content-type]
    (log/info (str "Requested file scan for " file-key ", to bucket " (:s3-bucket this)))
    (.sendMessage (:sqs-client this) (:request-queue-url this)
                  (json/generate-string {:objects [{
                                                    :bucket (:s3-bucket this)
                                                    :key file-key
                                                    :custom_data (json/generate-string {:start-time (System/currentTimeMillis)
                                                                                        :filename filename
                                                                                        :content-type content-type})}]}))))

(defn new-scanner []
  (map->VirusScanner {}))
