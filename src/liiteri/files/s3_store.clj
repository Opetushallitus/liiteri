(ns liiteri.files.s3-store
  (:require [liiteri.files.file-store :as file-store])
  (:import (java.io ByteArrayInputStream)
           (com.amazonaws.services.s3.model ObjectMetadata)))

(defn- bucket-name [config]
  (get-in config [:file-store :s3 :bucket]))

(defrecord S3Store [s3-client config]
  file-store/StorageEngine

  (create-file [this file file-key]
    (.putObject (:s3-client s3-client) (bucket-name config) file-key file))

  (create-file-from-bytearray [this file-bytes file-key]
    (with-open [inputstream (ByteArrayInputStream. file-bytes)]
      (let [s3-metadata (ObjectMetadata.)]
        (.setContentLength s3-metadata (count file-bytes))
        (.putObject (:s3-client s3-client) (bucket-name config) file-key inputstream s3-metadata))))

  (delete-file [this file-key]
    (.deleteObject (:s3-client s3-client) (bucket-name config) file-key))

  (get-file [this file-key]
    (-> (.getObject (:s3-client s3-client) (bucket-name config) file-key)
        (.getObjectContent)))

  (get-size-and-file [this file-key]
    (let [s3-object (.getObject (:s3-client s3-client) (bucket-name config) file-key)
          length    (-> s3-object
                        (.getObjectMetadata)
                        (.getContentLength))
          content   (.getObjectContent s3-object)]
      {:size length
       :file content}))

  (file-exists? [this file-key]
    (.doesObjectExist (:s3-client s3-client) (bucket-name config) file-key)))

(defn new-store []
  (map->S3Store {}))
