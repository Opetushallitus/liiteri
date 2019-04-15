(ns liiteri.files.s3-client
  (:require [com.stuartsierra.component :as component]
            [liiteri.files.file-store :as file-store])
  (:import [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.regions Regions]
           [com.amazonaws.auth DefaultAWSCredentialsProviderChain]
           [com.amazonaws.auth.profile ProfileCredentialsProvider]))

(defn- credentials-provider [config]
  (if-let [profile-name (get-in config [:file-store :s3 :credentials-profile])]
    (new ProfileCredentialsProvider profile-name)
    (DefaultAWSCredentialsProviderChain/getInstance)))

(defn- region [config]
  (Regions/fromName (get-in config [:file-store :s3 :region])))

(defrecord S3Client [config]
  component/Lifecycle

  (start [this]
    (if (nil? (:s3-client this))
      (assoc this :s3-client (-> (AmazonS3Client/builder)
                                 (.withRegion (region config))
                                 (.withCredentials (credentials-provider config))
                                 (.build)))
      this))

  (stop [this]
    (when-let [client (:s3-client this)]
      (.shutdown client))
    (assoc this :s3-client nil)))

(defn new-client []
  (map->S3Client {}))
