(ns liiteri.files.s3-client
  (:require [com.stuartsierra.component :as component]
            [liiteri.files.file-store :as file-store])
  (:import [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.regions Regions]
           [com.amazonaws.auth SystemPropertiesCredentialsProvider]))

(defrecord S3Client []
  component/Lifecycle

  (start [this]
    (let [client (-> (AmazonS3Client/builder)
                     (.withRegion Regions/EU_WEST_1)
                     (.withCredentials (SystemPropertiesCredentialsProvider.))
                     (.build))]
      (assoc this :s3-client client)))

  (stop [this]
    (when-let [client (:s3-client this)]
      (.shutdown client))
    (assoc this :s3-client nil)))

(defn new-client []
  (->S3Client))
