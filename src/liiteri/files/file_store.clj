(ns liiteri.files.file-store
  (:require [liiteri.db.file-metadata-store :as metadata-store]))

(defprotocol StorageEngine
  (create-file [this file file-key])

  (create-file-from-bytearray [this file-bytes file-key])

  (delete-file [this file-key])

  (get-file [this file-key])

  (get-size-and-file [this file-key]))

(defn create-metadata [file key application-key conn]
  (let [file-spec (assoc (select-keys file [:filename :content-type :size]) :key key :application-key application-key)]
    (metadata-store/create-file file-spec conn)))

(defn delete-preview-and-metadata [key storage-engine conn]
  (let [deleted (metadata-store/delete-preview key conn)]
    (when (> deleted 0)
      (.delete-file storage-engine key))
    deleted))

(defn delete-file-and-metadata [key storage-engine conn]
  (let [deleted (metadata-store/delete-file key conn)]
    (when (> deleted 0)
      (.delete-file storage-engine key)
      (when-let [previews (metadata-store/get-previews key conn)]
        (doseq [preview previews]
          (delete-preview-and-metadata (:key preview) storage-engine conn))))
    deleted))

(defn get-file-and-metadata [key storage-engine conn]
  (let [metadata (metadata-store/get-normalized-metadata! [key] conn)]
    (when (> (count metadata) 0)
      {:body     (.get-file storage-engine key)
       :filename (:filename (first metadata))})))

(defn get-file-keys-by-application-keys [application-keys conn]
  (metadata-store/get-file-keys-by-application-keys application-keys conn))

(defn delete-files-and-metadata-by-application-keys [application-keys storage-engine conn]
  (let [keys-to-delete (get-file-keys-by-application-keys application-keys conn)
        deleted-keys (doall
                       (map #(when (= 1 (delete-file-and-metadata (:key %) storage-engine conn)) %) keys-to-delete))]
    (println "original:" deleted-keys)
    (println "result:" (vec (map :key deleted-keys)))
    (vec (map :key deleted-keys))
    ))
