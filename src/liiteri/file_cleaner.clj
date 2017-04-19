(ns liiteri.file-cleaner
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.files.file-store :as file-store]
            [org.httpkit.client :as http]
            [ring.util.http-response :as response]
            [taoensso.timbre :as log]))

(defn- clean-files [db storage-engine config]
  (log/info "Cleaning files")
  (jdbc/with-db-transaction [datasource db]
    (doseq [file (metadata-store/get-old-draft-files db)]
      (log/info (str "Cleaning file: " (:key file)))
      (file-store/delete-file (:key file) storage-engine db))))

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
