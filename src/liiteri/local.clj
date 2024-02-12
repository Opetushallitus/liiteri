(ns liiteri.local
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log]
            [cheshire.core :as json])
  (:import [com.amazonaws.services.sqs.model ReceiveMessageRequest]
           [java.util.concurrent Executors TimeUnit ScheduledFuture]))

(def ^:private mock-filename-virus-pattern #"(?i)eicar|virus")

(defn- dev? []
  (= (:dev? env) "true"))

(defn- get-scan-request-queue-name [config]
  (get-in config [:bucketav :scan-request-queue-name]))

(defn- get-scan-result-queue-name [config]
  (get-in config [:bucketav :scan-result-queue-name]))

(defn- get-queue-url [sqs-client queue-name]
  (log/info (str "checking if " queue-name " present"))
  (let [queue-urls (-> (.listQueues sqs-client queue-name)
                      (.getQueueUrls))]
    (if (.isEmpty queue-urls)
      (do
        (log/info "creating " queue-name)
        (-> (.createQueue sqs-client queue-name)
            (.getQueueUrl)))
      (.get queue-urls 0))))

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

(defrecord Local [config sqs-client]
  component/Lifecycle

  (start [this]
    (if (dev?)
      (do
        (log/info "Setting up local environment" this)
        (let [request-queue-url (get-queue-url (:sqs-client sqs-client) (get-scan-request-queue-name config))
              result-queue-url (get-queue-url (:sqs-client sqs-client) (get-scan-result-queue-name config))
              scheduler (Executors/newScheduledThreadPool 1)
              future (.scheduleAtFixedRate
                       scheduler
                       #(poll-scan-requests (:sqs-client sqs-client) request-queue-url result-queue-url)
                       0 1 TimeUnit/SECONDS)]
          (assoc this :future future)))
      this))

  (stop [this]
    (when-let [^ScheduledFuture future (:future this)]
      (log/info "Cancelling polling")
      (.cancel future true))))

(defn new-local []
  (map->Local {}))