(ns liiteri.mime-fixer
  (:require [liiteri.db.file-metadata-store :as metadata-store]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [liiteri.mime :as mime]
            [taoensso.timbre :as log]
            [liiteri.files.file-store :as file-store])
  (:import (java.util.concurrent Executors TimeUnit ScheduledFuture)
           (org.apache.tika.io TikaInputStream)
           (java.io InputStream)))

(def mime-type-for-failed-cases "application/octet-stream")

(defn- log-mime-type-fix-result [file-key filename content-type status elapsed-time]
  (let [status-str (if (= status :successful) "OK" "FAILED")]
    (log/info (str "Mime type fix took " elapsed-time " ms, status " status-str " for file " filename " with key " file-key " (" content-type ")"))))

(defn fix-mime-type-of-file [conn
                             storage-engine
                             {file-key :key
                              filename :filename
                              uploaded :uploaded}]
  (let [start-time (System/currentTimeMillis)]
    (try
      (log/info (str "Fixing mime type of '" filename "' with key '" file-key "', uploaded on " uploaded " ..."))
      (with-open [^InputStream file (file-store/get-file storage-engine file-key)]
        (let [detected-content-type (mime/detect-mime-type (TikaInputStream/get file))
              fixed-filename (mime/fix-extension filename detected-content-type)
              names-for-logging (if (= filename fixed-filename)
                                  fixed-filename
                                  (str fixed-filename " (originally '" filename "')"))]
          (.readAllBytes file)
          (metadata-store/set-content-type-and-filename! file-key fixed-filename detected-content-type conn)
          (log-mime-type-fix-result file-key names-for-logging detected-content-type :successful (- (System/currentTimeMillis) start-time))
          true))
      (catch Exception e
        (log/error e (str "Failed to fix mime type of file '" filename "' with key '" file-key "', uploaded on " uploaded " : " (.getMessage e)))
        (metadata-store/set-content-type-and-filename! file-key filename mime-type-for-failed-cases conn)
        (log-mime-type-fix-result file-key
                                  filename
                                  mime-type-for-failed-cases
                                  :failed
                                  (- (System/currentTimeMillis) start-time))
        false))))

(defn- fix-mime-type-of-next-file [db storage-engine]
  (try
    (jdbc/with-db-transaction [tx db]
                              (let [conn {:connection tx}]
                                (if-let [file (metadata-store/get-file-without-mime-type conn)]
                                  (fix-mime-type-of-file conn storage-engine file)
                                  false)))
    (catch Exception e
      (log/error e "Failed to fix mime type of the next file")
      false)))

(defn- fix-mime-types-of-files [db storage-engine]
  (loop []
    (when (fix-mime-type-of-next-file db storage-engine)
      (recur))))

(defprotocol Fixer
  (fix-mime-types-of-files! [this]))

(defrecord MimeFixer [db storage-engine config]
  component/Lifecycle

  (start [this]
    (log/info "Starting MIME type fixing process...")
    (let [poll-interval (get-in config [:mime-fixer :poll-interval-seconds])
          scheduler (Executors/newScheduledThreadPool 1)
          fixer #(fix-mime-types-of-files db storage-engine)
          time-unit TimeUnit/SECONDS
          fixer-future (.scheduleAtFixedRate scheduler fixer 0 poll-interval time-unit)]
      (log/info (str "Started MIME type fixing process, restarting at " poll-interval " " time-unit " intervals."))
      (assoc this :fixer-future fixer-future)))

  (stop [this]
    (when-let [^ScheduledFuture fixer-future (:fixer-future this)]
      (.cancel fixer-future true))
    (log/info "Stopped MIME type fixing process")
    (assoc this :fixer-future nil))

  Fixer

  (fix-mime-types-of-files! [this]
    (fix-mime-types-of-files db storage-engine)))

(defn new-mime-fixer []
  (map->MimeFixer {}))
