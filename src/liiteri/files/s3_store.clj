(ns liiteri.files.s3-store
  (:require [liiteri.files.file-store :as file-store]))

(defrecord S3Store [s3-client]
  file-store/StorageEngine

  (create-file [this file file-key]
    (.putObject (:s3-client s3-client) "oph-liiteri-dev" file-key file))

  (delete-file [this file-key]
    (.deleteObject (:s3-client s3-client) "oph-liiteri-dev" file-key))

  (get-file [this file-key]
    (-> (.getObject (:s3-client s3-client) "oph-liiteri-dev" file-key)
        (.getObjectContent))))

(defn new-store []
  (map->S3Store {}))
