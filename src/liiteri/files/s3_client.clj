(ns liiteri.files.s3-client
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:import [software.amazon.awssdk.auth.credentials DefaultCredentialsProvider AwsBasicCredentials StaticCredentialsProvider]
           [software.amazon.awssdk.regions Region]
           [software.amazon.awssdk.services.s3 S3Client S3ClientBuilder]
           [java.net URI]))

(defn- dev? []
  (= (:dev? env) "true"))

(def aws-credentials
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)})

(defrecord AWSS3Client [config]
  component/Lifecycle

  (start [this]
    (if (nil? (:s3-client this))
      (assoc this :s3-client
                  (if (dev?)
                    (-> (S3Client/builder)
                        (.endpointOverride (URI/create "http://s3.localhost.localstack.cloud:4566"))
                        (.region (Region/US_EAST_1))
                        (.credentialsProvider
                          (StaticCredentialsProvider/create
                            (AwsBasicCredentials/create (:access-key aws-credentials)
                                                        (:secret-key aws-credentials))))
                        (.build))
                    (-> (S3Client/builder)
                        (.region (Region/of (get-in config [:file-store :s3 :region])))
                        (.credentialsProvider
                          (.build (DefaultCredentialsProvider/builder)))
                        (.build))))
      this))

  (stop [this]
    (assoc this :s3-client nil)))

(defn new-client []
  (map->AWSS3Client {}))
