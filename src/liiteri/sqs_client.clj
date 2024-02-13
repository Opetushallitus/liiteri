(ns liiteri.sqs-client
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:import [com.amazonaws.services.sqs AmazonSQSClient]
           [com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration]
           [com.amazonaws.auth AWSStaticCredentialsProvider]
           [com.amazonaws.auth BasicAWSCredentials]))

(def aws-credentials
  {:access-key (env :aws-access-key)
   :secret-key (env :aws-secret-key)})

(defn- get-sqs-client []
  (-> (AmazonSQSClient/builder)
      (.withEndpointConfiguration (AwsClientBuilder$EndpointConfiguration. "http://localhost:4566" "us-east-1"))
      (.withCredentials (AWSStaticCredentialsProvider. (BasicAWSCredentials. (:access-key aws-credentials) (:secret-key aws-credentials))))
      (.build)))

(defrecord SQSClient [config]
  component/Lifecycle

  (start [this]
    (let [sqs-client (get-sqs-client)]
      (assoc this :sqs-client sqs-client)))

  (stop [this]
    (assoc this :sqs-client nil)))

(defn new-client []
  (map->SQSClient {}))