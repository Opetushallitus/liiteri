(ns liiteri.sqs-client
  (:require [environ.core :refer [env]])
  (:import [software.amazon.awssdk.auth.credentials DefaultCredentialsProvider AwsBasicCredentials StaticCredentialsProvider]
           [software.amazon.awssdk.regions Region]
           [software.amazon.awssdk.services.sqs SqsClient]
           [java.net URI]))

(defn- dev? []
  (= (:dev? env) "true"))

(def aws-credentials
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)})

(defn get-sqs-client [config]
  (if (dev?)
    (-> (SqsClient/builder)
        (.endpointOverride (URI/create "http://sqs.localhost.localstack.cloud:4566"))
        (.region (Region/US_EAST_1))
        (.credentialsProvider
          (StaticCredentialsProvider/create
           (AwsBasicCredentials/create (:access-key aws-credentials)
                                       (:secret-key aws-credentials))))
        (.build))
    (-> (SqsClient/builder)
        (.credentialsProvider
          (.build (DefaultCredentialsProvider/builder)))
        (.build))))
