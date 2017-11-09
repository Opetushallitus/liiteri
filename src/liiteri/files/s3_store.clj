(ns liiteri.files.s3-store
  (:require [liiteri.files.file-store :as file-store]))

(defn- bucket-name [config]
  (get-in config [:file-store :s3 :bucket]))

(defrecord S3Store [s3-client config]
  file-store/StorageEngine

  (create-file [this file file-key]
    (.putObject (:s3-client s3-client) (bucket-name config) file-key file))

  (delete-file [this file-key]
    (.deleteObject (:s3-client s3-client) (bucket-name config) file-key))

  (get-file [this file-key]
    (-> (.getObject (:s3-client s3-client) (bucket-name config) file-key)
        (.getObjectContent))))

(defn new-store []
  (map->S3Store {}))
