(ns liiteri.virus-scan
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.files.file-store :as file-store]
            [liiteri.sqs-client :refer [get-sqs-client]]
            [taoensso.timbre :as log]
            [cheshire.core :as json])
  (:import [software.amazon.awssdk.services.sqs.model ReceiveMessageRequest GetQueueUrlRequest SendMessageRequest DeleteMessageRequest]
           [java.util.concurrent Executors TimeUnit ScheduledFuture]))

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
     (let [messages (-> (.receiveMessage sqs-client
                                         (-> (ReceiveMessageRequest/builder)
                                             (.queueUrl result-queue-url)
                                             (.waitTimeSeconds (int 1)) ; wait time of 1 second is to enable long polling which means we get answers from all sqs servers
                                             (.build)))
                        (.messages))]
       (log/info (str "Received " (.size messages) " virus scan results"))
       (doseq [message messages]
         (try
           (let [message (json/parse-string (.body message) true)
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
                                                            (file-store/delete-file-and-metadata file-key "liiteri-virus-scan" storage-engine conn {} false)
                                                            (metadata-store/set-virus-scan-status! file-key "virus_found" conn))
                                               (mark-and-log-failure file-key filename content-type 0 0 conn)))))))
           (.deleteMessage sqs-client (-> (DeleteMessageRequest/builder)
                                          (.queueUrl result-queue-url)
                                          (.receiptHandle (.receiptHandle message))
                                          (.build)))
           (catch Exception e
             (log/error e (str "Failed to process scan result for message: " (.body message))))))
       (.size messages))
     (catch Exception e
       (log/error e "Failed to process messages from scan result queue")
       0)
     (catch Throwable t
       (log/error t "Catastrophically failed to process messages from scan result queue")
       (throw t))))

(defprotocol Scanner
  (request-file-scan [this metadata]))

(defrecord VirusScanner [db storage-engine config]
  component/Lifecycle

  (start [this]
    (let [sqs-request-scan-client (get-sqs-client)
          sqs-poll-results-client (get-sqs-client)
          request-queue-name (get-in config [:bucketav :scan-request-queue-name])
          request-queue-url (-> (.getQueueUrl sqs-request-scan-client
                                              (-> (GetQueueUrlRequest/builder)
                                                  (.queueName request-queue-name)
                                                  (.build)))
                                (.queueUrl))
          result-queue-name (get-in config [:bucketav :scan-result-queue-name])
          result-queue-url (-> (.getQueueUrl sqs-poll-results-client
                                             (-> (GetQueueUrlRequest/builder)
                                                 (.queueName result-queue-name)
                                                 (.build)))
                                (.queueUrl))
          poll-interval (get-in config [:bucketav :poll-interval-seconds])
          s3-bucket (get-in config [:file-store :s3 :bucket])

          scheduler (Executors/newScheduledThreadPool 1)
          virus-scan #(while (< 0 (poll-scan-results sqs-poll-results-client result-queue-url db storage-engine config)))
          time-unit TimeUnit/SECONDS
          virus-scan-future (.scheduleAtFixedRate scheduler virus-scan 0 poll-interval time-unit)]
      (log/info (str "Started virus scan results polling process, restarting at " poll-interval " " time-unit " intervals."))
      (assoc this :virus-scan-future virus-scan-future
                  :request-queue-url request-queue-url
                  :sqs-request-scan-client sqs-request-scan-client
                  :s3-bucket s3-bucket)))

  (stop [this]
    (when-let [^ScheduledFuture virus-scan-future (:virus-scan-future this)]
      (.cancel virus-scan-future true))
    (log/info "Stopped virus scan results polling")
    (assoc this :virus-scan-future nil
                :request-queue-url nil
                :sqs-request-scan-client nil
                :s3-bucket nil))

  Scanner

  (request-file-scan [this metadata]
    (when (not-empty metadata)
      (doseq [file metadata]
        (log/info (str "Requesting file scan for " (:key file) ", to bucket " (:s3-bucket this))))
      (doseq [metadatapart (partition 10 10 [] metadata)]   ; BucketAV hyväksyy maksimissaan 10 tiedostoa kerrallaan
        (.sendMessage (:sqs-request-scan-client this)
                      (-> (SendMessageRequest/builder)
                          (.queueUrl (:request-queue-url this))
                          (.messageBody
                            (json/generate-string
                              {:objects
                               (map (fn [file]
                                      {:bucket (:s3-bucket this)
                                       :key (:key file)
                                       :custom_data (json/generate-string {:start-time (System/currentTimeMillis)
                                                                           :filename (:filename file)
                                                                           :content-type (:content-type file)})})
                                    metadatapart)}))
                          (.build)))))))

(defn new-scanner []
  (map->VirusScanner {}))
