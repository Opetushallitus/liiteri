(ns liiteri.sqs-client
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as log])
  (:import [com.amazonaws.services.sqs AmazonSQSClient]
           [com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration]
           [com.amazonaws.auth DefaultAWSCredentialsProviderChain]))

(defn- dev? []
  (= (:dev? env) "true"))

(defn- get-sqs-client []
  (-> (AmazonSQSClient/builder)
      (.withEndpointConfiguration (AwsClientBuilder$EndpointConfiguration. "http://localhost:4566" "us-east-1"))
      (.withCredentials (DefaultAWSCredentialsProviderChain/getInstance))
      (.build)))

(defrecord SQSClient [config]
  component/Lifecycle

  (start [this]
    (let [sqs-client (get-sqs-client)]
      (assoc this :sqs-client sqs-client)))

  (stop [this]
    (when-let [^AmazonSQSClient sqs-client (:sqs-client this)]
      (.shutdown sqs-client))
    (assoc this :sqs-client nil)))

(defn new-client []
  (map->SQSClient {}))