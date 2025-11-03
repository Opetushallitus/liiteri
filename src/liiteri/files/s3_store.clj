(ns liiteri.files.s3-store
  (:require [liiteri.files.file-store :as file-store])
  (:import [java.io ByteArrayInputStream]
           [software.amazon.awssdk.services.s3.model PutObjectRequest DeleteObjectRequest GetObjectRequest HeadObjectRequest NoSuchKeyException]
           [software.amazon.awssdk.core.sync RequestBody]))

(defn- bucket-name [config]
  (get-in config [:file-store :s3 :bucket]))

(defrecord S3Store [s3-client config]
  file-store/StorageEngine

  (create-file [this file file-key]
    (.putObject (:s3-client s3-client)
                (-> (PutObjectRequest/builder)
                    (.bucket (bucket-name config))
                    (.key file-key)
                    (.build))
                (RequestBody/fromFile file)))

  (create-file-from-bytearray [this file-bytes file-key]
    (with-open [inputstream (ByteArrayInputStream. file-bytes)]
      (let [length (long (count file-bytes))]
        (.putObject (:s3-client s3-client)
                    (-> (PutObjectRequest/builder)
                        (.bucket (bucket-name config))
                        (.key file-key)
                        (.build))
                    (RequestBody/fromInputStream inputstream length)))))

  (delete-file [this file-key]
    (.deleteObject (:s3-client s3-client)
                   (-> (DeleteObjectRequest/builder)
                       (.bucket (bucket-name config))
                       (.key file-key)
                       (.build))))

  (get-file [this file-key]
    (.getObject (:s3-client s3-client)
                (-> (GetObjectRequest/builder)
                    (.bucket (bucket-name config))
                    (.key file-key)
                    (.build))))

  (get-size-and-file [this file-key]
    (let [s3-object (.getObject (:s3-client s3-client)
                                (-> (GetObjectRequest/builder)
                                    (.bucket (bucket-name config))
                                    (.key file-key)
                                    (.build)))
          length    (-> s3-object
                        (.response)
                        (.getContentLength))]
      {:size length
       :file s3-object}))

  (file-exists? [this file-key]
    (try
      (some?
        (.headObject (:s3-client s3-client)
                     (-> (HeadObjectRequest/builder)
                         (.bucket (bucket-name config))
                         (.key file-key)
                         (.build))))
      (catch NoSuchKeyException e
        false))))

(defn new-store []
  (map->S3Store {}))
