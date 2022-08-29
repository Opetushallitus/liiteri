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

(defn- clean-file [conn storage-engine file process-name delete-file-permanently?]
  (log/info (str process-name " cleaning file: " (:key file)))
  (try
    (file-store/delete-file-and-metadata (:key file) process-name storage-engine conn delete-file-permanently?)
    (catch Exception e
      (log/error e (str "Failed to clean file " (:key file))))))

(defn- clean-preview [conn storage-engine preview]
  (log/info (str "Cleaning preview: " (:key preview)))
  (try
    (file-store/delete-preview-and-metadata (:key preview) storage-engine conn false)
    (catch Exception e
      (log/error e (str "Failed to clean preview " (:key preview))))))

(defn- clean-next-file [db storage-engine]
  (try
    (jdbc/with-db-transaction [tx db]
      (let [conn              {:connection tx}
            old-draft-file    (metadata-store/get-old-draft-file conn)
            old-draft-preview (metadata-store/get-old-draft-preview conn)]
        (and old-draft-file
             (clean-file conn storage-engine old-draft-file "liiteri-file-cleaner" false)
             old-draft-preview
             (clean-preview conn storage-engine old-draft-preview))))
    (catch Exception e
      (log/error e "Failed to clean the next file"))))

(defn- clean-files [db storage-engine]
  (log/info "Cleaning files")
  (loop []
    (when (clean-next-file db storage-engine)
      (recur)))
  (log/info "Finished cleaning files"))

(defn- clean-draft-files [db storage-engine times]
  (log/info "Starting file cleaner process")
  (a/go-loop []
    (when-let [_ (a/<! times)]
      (clean-files db storage-engine)
      (recur))))

(defn- clean-next-deleted-file [db storage-engine]
  (try
    (jdbc/with-db-transaction [tx db]
                              (let [conn              {:connection tx}
                                    old-deleted-file  (metadata-store/get-old-deleted-file conn)]
                                (and old-deleted-file
                                     (clean-file conn storage-engine old-deleted-file "liiteri-deleted-cleaner" true))))
    (catch Exception e
      (log/error e "Failed to clean the next file"))))

(defn- clean-deleted-files [db storage-engine times]
  (log/info "Starting deleted file cleaner process")
  (a/go-loop []
    (when-let [_ (a/<! times)]
      (log/info "Cleaning deleted files")
      (loop []
        (when (clean-next-deleted-file db storage-engine)
          (recur)))
      (log/info "Finished cleaning deleted files")
      (recur))))

(defrecord FileCleaner [db storage-engine config clean-deleted-files?]
  component/Lifecycle

  (start [this]
    (let [poll-interval (if clean-deleted-files?
                          (get-in config [:file-delete-cleaner :poll-interval-seconds])
                          (get-in config [:file-cleaner :poll-interval-seconds]))
          times         (c/chime-ch (p/periodic-seq (t/now) (t/seconds poll-interval))
                                    {:ch (a/chan (a/sliding-buffer 1))})]
      (if clean-deleted-files?
        (clean-deleted-files db storage-engine times)
        (clean-draft-files db storage-engine times))
      (assoc this :chan times)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan))
    (log/info "Stopped file cleaner process")
    (assoc this :chan nil)))

(defn new-cleaner [clean-deleted-files?]
  (map->FileCleaner {:clean-deleted-files? clean-deleted-files?}))
