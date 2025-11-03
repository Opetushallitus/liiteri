(ns liiteri.auth.cas-client
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clj-http.client :as http-client]
            [taoensso.timbre :as log])
  (:import [fi.vm.sade.javautils.nio.cas CasClientBuilder CasConfig$CasConfigBuilder]))

(def csrf-value "liiteri")
(def caller-id "1.2.246.562.10.00000000001.liiteri.backend")

(defn enrich-with-mandatory-headers-and-common-settings [opts]
  (-> opts
      (update :connection-timeout (fnil identity 60000))
      (update :socket-timeout (fnil identity 60000))
      (assoc  :throw-exceptions false)
      (update :headers merge
              {"Caller-Id" caller-id}
              {"CSRF" csrf-value})
      (update :cookies merge {"CSRF" {:value csrf-value :path "/"}})))

(defrecord CasClientState [client session-cookie-name session-id])

(defn new-cas-client [config]
  (let [{username :username
         password :password} (-> config :cas)
        cas-url (str (-> config :virkailija-host) "/cas")
        cas-config (-> (new CasConfig$CasConfigBuilder username password cas-url "" csrf-value caller-id "")
                       (.setJsessionName "JSESSIONID")
                       (.build))
        cas-client (CasClientBuilder/build cas-config)]
    cas-client))
