(ns liiteri.virus-scan
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.files.file-store :as file-store]
            [clj-http.client :as http-client]
            [ring.util.http-response :as response]
            [taoensso.timbre :as log])
  (:import java.util.concurrent.TimeUnit))

(def ^:private successful-resp-body "Everything ok : true\n")
(def ^:private failed-resp-body "Everything ok : false\n")
(def ^:private mock-filename-virus-pattern #"(?i)eicar|virus")

(defn- mock-enabled? [config]
  (true? (get-in config [:antivirus :mock?])))

(defn- wait-randomly []
  (Thread/sleep (rand-int 10000)))

(defn- mock-scan-file [filename]
  (let [scan-failed (re-find mock-filename-virus-pattern filename)
        result      (if scan-failed
                      failed-resp-body
                      successful-resp-body)]
    (response/ok result)))

(defn- log-virus-scan-result [file-key filename content-type config status elapsed-time]
  (let [status-str (if (= status :successful) "OK" "FAILED")]
    (log/info (str "Virus scan took " elapsed-time " ms, status " status-str " for file " filename " with key " file-key " (" content-type ")"
                   (when (mock-enabled? config) ", virus scan process in mock mode")))))

(defn- scan-file [conn
                  storage-engine
                  config
                  {file-key :key
                   filename :filename
                   content-type :content-type}]
  (let [clamav-url (str (get-in config [:antivirus :clamav-url]) "/scan")]
    (try
      (log/info (str "Virus scan for " filename " with key " file-key))
      (let [file (.get-file storage-engine file-key)
            start-time (System/currentTimeMillis)
            scan-result (if (mock-enabled? config)
                          (mock-scan-file filename)
                          (http-client/post clamav-url {:multipart        [{:name "file" :content file :filename filename}
                                                                           {:name "name" :content filename}]
                                                        :throw-exceptions false
                                                        :socket-timeout   (.toMillis TimeUnit/MINUTES 22)
                                                        :conn-timeout     (.toMillis TimeUnit/SECONDS 2)}))
            elapsed-time (- (System/currentTimeMillis) start-time)]
        (cond (= (:status scan-result) 200)
              (if (= (:body scan-result) "Everything ok : true\n")
                (do
                  (log-virus-scan-result file-key filename content-type config :successful elapsed-time)
                  (metadata-store/set-virus-scan-status! file-key :done conn))
                (do
                  (log-virus-scan-result file-key filename content-type config :failed elapsed-time)
                  (file-store/delete-file-and-metadata file-key storage-engine conn)
                  (metadata-store/set-virus-scan-status! file-key :failed conn)))
              (= (:status scan-result) 503)
              (log/warn "Failed to scan file" filename "with key" file-key ": Service Unavailable")
              :else
              (do
                (when (= (metadata-store/mark-virus-scan-for-retry-or-fail file-key conn) "failed")
                  (log/error "FINAL: Scan of file " filename " with key " file-key " (" content-type ") will not be retried"))
                (log/error (str "Failed to scan file " filename " with key " file-key ": " scan-result)))))
      (catch Exception e
        (when (= (metadata-store/mark-virus-scan-for-retry-or-fail file-key conn) "failed")
          (log/error "FINAL: Scan of file " filename " with key " file-key " (" content-type ") will not be retried"))
        (log/error e (str "Failed to scan file " filename " with key " file-key " (" content-type ") using Clamav at " clamav-url ", will retry"))))))


(defn- scan-next-file [db storage-engine config]
  (try
    (jdbc/with-db-transaction [tx db]
      (let [conn {:connection tx}]
        (when-let [file (metadata-store/get-unscanned-file conn)]
          (scan-file conn storage-engine config file))))
    (catch Exception e
      (log/error e "Failed to scan the next file"))))

(defn- scan-files [db storage-engine config]
  (loop []
    (when (mock-enabled? config)
      (wait-randomly))
    (when (scan-next-file db storage-engine config)
      (recur))))

(defprotocol Scanner
  (scan-files! [this]))

(defrecord VirusScanner [db storage-engine config]
  component/Lifecycle

  (start [this]
    (let [poll-interval (get-in config [:antivirus :poll-interval-seconds])
          times         (c/chime-ch (p/periodic-seq (t/now) (t/seconds poll-interval))
                                    {:ch (a/chan (a/sliding-buffer 1))})]
      (log/info "Starting virus scan process")
      (a/go-loop []
        (when-let [_ (a/<! times)]
          (scan-files db storage-engine config)
          (recur)))
      (assoc this :chan times)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan))
    (log/info "Stopped virus scan process")
    (assoc this :chan nil))

  Scanner

  (scan-files! [this]
    (scan-files db storage-engine config)))

(defn new-scanner []
  (map->VirusScanner {}))
