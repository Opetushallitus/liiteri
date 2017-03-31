(ns liiteri.virus-scan
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store]
            [org.httpkit.client :as http]
            [taoensso.timbre :as log]))

(defn- scan-file [db storage-engine config]
  (jdbc/with-db-transaction [datasource db]
    (let [conn {:connection db}]
      (when-let [{file-key :key filename :filename content-type :content-type} (metadata-store/get-unscanned-file conn)]
        (let [file        (.get-file storage-engine file-key)
              clamav-url  (str (get-in config [:antivirus :clamav-url]) "/scan")
              scan-result @(http/post clamav-url {:form-params {"name" filename}
                                                  :multipart   [{:name "file" :content file :filename filename}]})]
          (when (= (:status scan-result) 200)
            (if (= (:body scan-result) "Everything ok : true\n")
              (do
                (log/info (str "ClamAV scan OK for file " filename " (" content-type ")"))
                (metadata-store/set-virus-scan-status! file-key :done conn))
              (do
                (log/info (str "ClamAV scan FAILED for file " filename " (" content-type ")"))
                (metadata-store/set-virus-scan-status! file-key :failed conn)))))))))

(defn- scan-files [db storage-engine config]
  (loop []
    (when (scan-file db storage-engine config)
      (recur))))

(defrecord Scanner [db storage-engine config]
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
    (assoc this :chan nil)))

(defn new-scanner []
  (map->Scanner {}))
