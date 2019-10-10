(ns liiteri.mime-fixer
  (:require [liiteri.db.file-metadata-store :as metadata-store]
            [chime :as c]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [liiteri.mime :as mime]
            [taoensso.timbre :as log]))

(def mime-type-for-failed-cases "application/octet-stream")

(defn- log-mime-type-fix-result [file-key filename content-type status elapsed-time]
  (let [status-str (if (= status :successful) "OK" "FAILED")]
    (log/info (str "Mime type fix took " elapsed-time " ms, status " status-str " for file " filename " with key " file-key " (" content-type ")"))))

(defn- fix-mime-type-of-file [conn
                              storage-engine
                              {file-key :key
                               filename :filename
                               uploaded :uploaded}]
  (let [start-time (System/currentTimeMillis)]
    (try
      (log/info (str "Fixing mime type of '" filename "' with key '" file-key "', uploaded on " uploaded " ..."))
      (let [file (.get-file storage-engine file-key)
            detected-content-type (mime/detect-mime-type file)
            elapsed-time (- (System/currentTimeMillis) start-time)]
        (metadata-store/set-content-type! file-key detected-content-type conn)
        (log-mime-type-fix-result file-key filename detected-content-type :successful elapsed-time))
      (catch Exception e
        (log/error e (str "Failed to fix mime type of file '" filename "' with key '" file-key "', uploaded on " uploaded))
        (metadata-store/set-content-type! file-key mime-type-for-failed-cases conn)
        (log-mime-type-fix-result file-key filename mime-type-for-failed-cases :failed (- (System/currentTimeMillis) start-time))))))

(defn- fix-mime-type-of-next-file [db storage-engine]
  (try
    (jdbc/with-db-transaction [tx db]
                              (let [conn {:connection tx}]
                                (when-let [file (metadata-store/get-file-without-mime-type conn)]
                                  (fix-mime-type-of-file conn storage-engine file))))
    (catch Exception e
      (log/error e "Failed to fix mime type of the next file"))))

(defn- fix-mime-types-of-files [db storage-engine]
  (loop []
    (when (fix-mime-type-of-next-file db storage-engine)
      (recur))))

(defprotocol Fixer
  (fix-mime-types-of-files! [this]))

(defrecord MimeFixer [db storage-engine config]
  component/Lifecycle

  (start [this]
    (let [poll-interval (get-in config [:antivirus :poll-interval-seconds])
          times (c/chime-ch (p/periodic-seq (t/now) (t/seconds poll-interval))
                            {:ch (a/chan (a/sliding-buffer 1))})]
      (log/info "Starting MIME type fixing process")
      (a/go-loop []
        (when-let [_ (a/<! times)]
          (fix-mime-types-of-files db storage-engine)
          (recur)))
      (assoc this :chan times)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan))
    (log/info "Stopped MIME type fixing process")
    (assoc this :chan nil))

  Fixer

  (fix-mime-types-of-files! [this]
    (fix-mime-types-of-files db storage-engine)))

(defn new-mime-fixer []
  (map->MimeFixer {}))
