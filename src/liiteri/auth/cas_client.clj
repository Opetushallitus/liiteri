(ns liiteri.auth.cas-client
  (:import [fi.vm.sade.javautils.nio.cas CasClientBuilder CasConfig$CasConfigBuilder]))

(def csrf-value "liiteri")
(def caller-id "1.2.246.562.10.00000000001.liiteri.backend")

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
