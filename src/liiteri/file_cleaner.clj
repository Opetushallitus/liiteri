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

(defn- clean-file [db storage-engine]
  (jdbc/with-db-transaction [tx db]
    (let [conn {:connection tx}]
      (when-let [file (metadata-store/get-old-draft-file conn)]
        (log/info (str "Cleaning file: " (:key file)))
        (try
          (file-store/delete-file-and-metadata (:key file) storage-engine conn)
          (catch Exception e
            (log/error e (str "Failed to delete file " (:key file)))))))))

(defn- clean-files [db storage-engine]
  (log/info "Cleaning files")
  (loop []
    (when (clean-file db storage-engine)
      (recur)))
  (log/info "Finished cleaning files"))

(defrecord FileCleaner [db storage-engine config]
  component/Lifecycle

  (start [this]
    (let [poll-interval (get-in config [:file-cleaner :poll-interval-seconds])
          times         (c/chime-ch (p/periodic-seq (t/now) (t/seconds poll-interval))
                                    {:ch (a/chan (a/sliding-buffer 1))})]
      (log/info "Starting file cleaner process")
      (a/go-loop []
        (when-let [_ (a/<! times)]
          (clean-files db storage-engine)
          (recur)))
      (assoc this :chan times)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan))
    (log/info "Stopped file cleaner process")
    (assoc this :chan nil)))

(defn new-cleaner []
  (map->FileCleaner {}))
