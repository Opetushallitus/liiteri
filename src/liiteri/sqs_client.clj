(ns liiteri.sqs-client
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:import [com.amazonaws.services.sqs AmazonSQSClient]
           [com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration]
           [com.amazonaws.auth DefaultAWSCredentialsProviderChain]
           [com.amazonaws.auth AWSStaticCredentialsProvider]
           [com.amazonaws.auth BasicAWSCredentials]))

(defn- dev? []
  (= (:dev? env) "true"))

(def aws-credentials
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)})

(defrecord SQSClient [config]
  component/Lifecycle

  (start [this]
    (assoc this :sqs-client
                (if (dev?)
                  (-> (AmazonSQSClient/builder)
                      (.withEndpointConfiguration (AwsClientBuilder$EndpointConfiguration. "http://localhost:4566" "us-east-1"))
                      (.withCredentials (AWSStaticCredentialsProvider. (BasicAWSCredentials. (:access-key aws-credentials) (:secret-key aws-credentials))))
                      (.build))
                  (-> (AmazonSQSClient/builder)
                      (.withCredentials (DefaultAWSCredentialsProviderChain/getInstance))
                      (.build)))))

  (stop [this]
    (assoc this :sqs-client nil)))

(defn new-client []
  (map->SQSClient {}))