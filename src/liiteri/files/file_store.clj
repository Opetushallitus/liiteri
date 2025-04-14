(ns liiteri.files.file-store
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [liiteri.db.file-metadata-store :as metadata-store]))

(defprotocol StorageEngine
  (create-file [this file file-key])

  (create-file-from-bytearray [this file-bytes file-key])

  (delete-file [this file-key])

  (get-file [this file-key])

  (get-size-and-file [this file-key])

  (file-exists? [this file-key]))

(defn create-metadata [file key origin-system origin-reference conn]
  (let [file-spec (assoc (select-keys file [:filename :content-type :size]) :key key :origin-system origin-system :origin-reference origin-reference)]
    (metadata-store/create-file file-spec conn)))

(defn delete-preview-and-metadata [key storage-engine conn delete-file-permanently?]
  (let [deleted (metadata-store/delete-preview key conn)]
    (when (or delete-file-permanently? (> deleted 0))
      (.delete-file storage-engine key))
    deleted))

(defn delete-file-and-metadata [key user storage-engine conn config delete-file-permanently?]
  (let [is-preserved? (boolean (some #(= % key) (string/split (get config :filekeys-not-to-be-deleted "") #",")))
        previews-with-key (if is-preserved? [] (metadata-store/get-previews key conn))
        deleted (if is-preserved? 0 (metadata-store/delete-file key user conn delete-file-permanently?))]
    (when (> deleted 0)
      (.delete-file storage-engine key)
      (when-let [previews previews-with-key]
        (doseq [preview previews]
          (delete-preview-and-metadata (:key preview) storage-engine conn delete-file-permanently?))))
    {:ignored (if is-preserved? 1 0) :deleted deleted}))

(defn get-file-and-metadata [key storage-engine conn]
  (let [metadata (metadata-store/get-normalized-metadata! [key] conn)]
    (when (> (count metadata) 0)
      {:body     (.get-file storage-engine key)
       :filename (:filename (first metadata))})))

(defn delete-files-and-metadata-by-origin-references [origin-references session storage-engine conn config]
  (let [all-preserved-keys (set (string/split (get config :filekeys-not-to-be-deleted "") #","))
        checked-keys (set (map :key (metadata-store/get-file-keys-by-origin-references origin-references conn)))
        keys-to-ignore (vec (set/intersection checked-keys all-preserved-keys))
        keys-to-delete (set/difference checked-keys all-preserved-keys)
        user (get-in session [:identity :oid])
        deleted-keys (filterv #(= 1 (:deleted (delete-file-and-metadata % user storage-engine conn {} false))) keys-to-delete)]
    {:deleted-keys deleted-keys :ignored-keys keys-to-ignore}))
