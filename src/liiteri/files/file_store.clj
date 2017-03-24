(ns liiteri.files.file-store
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store])
  (:import [java.util UUID]))

(defprotocol StorageEngine
  (create-file [this file file-key])

  (delete-file [this file-key])

  (get-file [this file-key]))

(defn create-file [file storage-engine db]
  (let [key     (str (UUID/randomUUID))
        _       (.create-file storage-engine (:tempfile file) key)]
    (jdbc/with-db-transaction [datasource db]
      (let [conn {:connection datasource}]
        (metadata-store/create-file (assoc (select-keys file [:filename :content-type :size]) :key key)
                                    conn)))))

(defn delete-file [key storage-engine db]
  (let [deleted (metadata-store/delete-file key db)]
    (when (> deleted 0)
      (.delete-file storage-engine key))
    deleted))

(defn get-file [key storage-engine db]
  (let [metadata (metadata-store/get-metadata key db)]
    (when (> (count metadata) 0)
      (.get-file storage-engine key))))
