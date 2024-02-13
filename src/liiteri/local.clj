(ns liiteri.local
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log]
            [cheshire.core :as json])
  (:import [com.amazonaws.services.sqs.model ReceiveMessageRequest]))

(def ^:private mock-filename-virus-pattern #"(?i)eicar|virus")

(defn- dev? []
  (= (:dev? env) "true"))

(defn- get-scan-request-queue-name [config]
  (get-in config [:bucketav :scan-request-queue-name]))

(defn- get-scan-result-queue-name [config]
  (get-in config [:bucketav :scan-result-queue-name]))

(defn- get-bucket-name [config]
       (get-in config [:file-store :s3 :bucket]))

(defn- ensure-queue-exists [sqs-client queue-name]
       (log/info (str "checking if " queue-name " present"))
       (let [queue-urls (-> (.listQueues sqs-client queue-name)
                       (.getQueueUrls))]
            (if (.isEmpty queue-urls)
              (do
                (log/info "creating " queue-name)
                (-> (.createQueue sqs-client queue-name)
                    (.getQueueUrl)))
              (.get queue-urls 0))))

(defn- ensure-bucket-exists [s3-client bucket-name]
       (log/info (str "checking if " bucket-name " present"))
       (let [buckets (.listBuckets s3-client)]
            (log/info (str "buckets: " buckets))
            (when (empty? (filter (fn [bucket] (= bucket-name (.getName bucket))) buckets))
                  (log/info (str "creating bucket " bucket-name))
                  (let [bucket (.createBucket s3-client bucket-name)]
                       (log/info (str "created bucket: " (.getName bucket)))))))

(defn- poll-scan-requests [sqs-client request-queue-url result-queue-url]
  (try
   (doseq [message (-> (.receiveMessage sqs-client (-> (ReceiveMessageRequest. request-queue-url)
                                                     (.withWaitTimeSeconds (int 0))))
                     (.getMessages))]
      (doseq [scan-request (:objects (json/parse-string (.getBody message) true))]
        (log/info (str "Received scan request: " scan-request))
        (let [custom-data (json/parse-string (:custom_data scan-request) true)
              filename (:filename custom-data)
              scan-failed (re-find mock-filename-virus-pattern filename)]
              (.sendMessage sqs-client result-queue-url (json/generate-string {:bucket (:bucket scan-request)
                                                                               :key (:key scan-request)
                                                                               :status (if scan-failed "infected" "clean")
                                                                               :custom_data (:custom_data scan-request)}))))
      (.deleteMessage sqs-client request-queue-url (.getReceiptHandle message)))
   (catch Exception e
     (log/error e "Failed to process scan request"))))

(defrecord Local [config sqs-client s3-client]
  component/Lifecycle

  (start [this]
    (if (dev?)
      (do
        (log/info "Setting up local environment" this)
        (let [request-queue-url (ensure-queue-exists (:sqs-client sqs-client) (get-scan-request-queue-name config))
              result-queue-url (ensure-queue-exists (:sqs-client sqs-client) (get-scan-result-queue-name config))
              times (c/chime-ch (p/periodic-seq (t/now) (t/seconds 1))
                                {:ch (a/chan (a/sliding-buffer 1))})]
             (ensure-bucket-exists (:s3-client s3-client) (get-bucket-name config))
             (a/go-loop []
               (when-let [_ (a/<! times)]
                         (poll-scan-requests (:sqs-client sqs-client) request-queue-url result-queue-url)
                         (recur)))
             (assoc this :chan times)))
      this))

  (stop [this]
        (when-let [chan (:chan this)]
                  (a/close! chan))))

(defn new-local []
  (map->Local {}))