(ns liiteri.file-cleaner
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.files.file-store :as file-store]
            [taoensso.timbre :as log]))

(defn- scan-file [db storage-engine config]
  (jdbc/with-db-transaction [datasource db]
    (let [conn {:connection db}]
      (when-let [file (metadata-store/get-old-draft-file db)]
        (log/info (str "Cleaning file: " (:key file)))
        (try
          (file-store/delete-file-and-metadata (:key file) storage-engine db)
          (catch Exception e
            (log/error e (str "Failed to delete file " (:key file)))))))))

(defn- clean-files [db storage-engine config]
  (loop []
    (log/info "Cleaning files")
    (when (scan-file db storage-engine config)
      (recur))))

(defrecord FileCleaner [db storage-engine config]
  component/Lifecycle

  (start [this]
    (let [poll-interval (get-in config [:file-cleaner :poll-interval-seconds])
          times         (c/chime-ch (p/periodic-seq (t/now) (t/seconds poll-interval))
                                    {:ch (a/chan (a/sliding-buffer 1))})]
      (log/info "Starting file cleaner process")
      (a/go-loop []
        (when-let [_ (a/<! times)]
          (clean-files db storage-engine config)
          (recur)))
      (assoc this :chan times)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan))
    (log/info "Stopped file cleaner process")
    (assoc this :chan nil)))

(defn new-cleaner []
  (map->FileCleaner {}))
