(ns liiteri.files.file-store
  (:require [clj-time.core :as t]
            [liiteri.db.file-metadata-store :as metadata-store])
  (:import [java.util UUID]))

(defprotocol StorageEngine
  (create-file [this file file-key])

  (create-file-from-bytearray [this file-bytes file-key])

  (delete-file [this file-key])

  (get-file [this file-key]))

(defn create-file-and-metadata [file storage-engine conn]
  (let [key       (str (UUID/randomUUID))
        file-spec (assoc (select-keys file [:filename :content-type :size]) :key key)]
    (.create-file storage-engine (:tempfile file) key)
    (metadata-store/create-file file-spec conn)))

(defn delete-file-and-metadata [key storage-engine conn]
  (let [deleted (metadata-store/delete-file key conn)]
    (when (> deleted 0)
      (.delete-file storage-engine key))
    deleted))

(defn get-file-and-metadata [key storage-engine conn]
  (let [metadata (metadata-store/get-metadata [key] conn)]
    (when (> (count metadata) 0)
      {:body (.get-file storage-engine key)
       :filename (:filename (first metadata))})))
