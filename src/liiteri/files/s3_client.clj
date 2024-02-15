(ns liiteri.files.s3-client
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:import [com.amazonaws.regions Regions]
           [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration]
           [com.amazonaws.auth DefaultAWSCredentialsProviderChain]
           [com.amazonaws.auth.profile ProfileCredentialsProvider]
           [com.amazonaws.auth AWSStaticCredentialsProvider]
           [com.amazonaws.auth BasicAWSCredentials]))

(defn- dev? []
  (= (:dev? env) "true"))

(defn- region [config]
  (Regions/fromName (get-in config [:file-store :s3 :region])))

(def aws-credentials
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)})

(defn- credentials-provider [config]
  (if-let [profile-name (get-in config [:file-store :s3 :credentials-profile])]
    (new ProfileCredentialsProvider profile-name)
    (AWSStaticCredentialsProvider. (BasicAWSCredentials. (:access-key aws-credentials) (:secret-key aws-credentials)))))

(defrecord S3Client [config]
  component/Lifecycle

  (start [this]
    (if (nil? (:s3-client this))
      (assoc this :s3-client
                  (if (dev?)
                    (-> (AmazonS3Client/builder)
                        (.withEndpointConfiguration (AwsClientBuilder$EndpointConfiguration. "http://localhost:4566" "us-east-1"))
                        (.withPathStyleAccessEnabled true)
                        (.withCredentials (credentials-provider config))
                        (.build))
                    (-> (AmazonS3Client/builder)
                        (.withRegion (region config))
                        (.withCredentials (DefaultAWSCredentialsProviderChain/getInstance))
                        (.build))))
      this))

  (stop [this]
    (assoc this :s3-client nil)))

(defn new-client []
  (map->S3Client {}))
