(ns liiteri.auth.auth
  (:require [clj-ring-db-session.authentication.login :as crdsa-login]
            [clj-ring-db-session.session.session-store :as crdsa-session-store]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [clojure.string :as s]
            [liiteri.urls :as urls]
            [cheshire.core :as json]
            [liiteri.auth.cas-client :as cas]
            [taoensso.timbre :as log])
  (:import (fi.vm.sade.javautils.nio.cas CasLogout)))

(defn cas-login [config cas-client ticket]
  (fn []
    (when ticket
      [(.get (.validateServiceTicketWithVirkailijaUserDetails
               cas-client
               (urls/liiteri-login-url config)
               ticket))
       ticket])))

(defn- login-failed
  ([config e]
   (log/error e "Error in login ticket handling")
   (resp/redirect (urls/redirect-to-login-failed-page-url config)))
  ([config]
   (resp/redirect (urls/redirect-to-login-failed-page-url config))))

(def oph-organization "1.2.246.562.10.00000000001")

(defn- role-starts-with-liiteri-crud?
  [role]
  (s/starts-with? role "ROLE_APP_LIITERI_CRUD_"))

(defn parse-liiteri-rights
  [roles]
  (if (some role-starts-with-liiteri-crud? roles)
    #{:liiteri-crud}
    #{}))

(defn parse-organization-oids
  [roles]
  (->> roles
       (filter role-starts-with-liiteri-crud?)
       (map #(last (s/split % #"_")))
       (filter #(re-matches #"1\.2\.246\.562\.[0-9]+\.[0-9]+" %))
       set))

(defn- login-succeeded [response userdetails]
  (let [roles (.getRoles userdetails)
        organization-oids (parse-organization-oids roles)
        rights (parse-liiteri-rights roles)]
    (update-in
      response
      [:session :identity]
      assoc
      :oid (.getHenkiloOid userdetails)
      :rights rights
      :superuser (contains? organization-oids oph-organization))))

(defn login [login-provider
             redirect-url
             config]
  (try
    (if-let [[userdetails ticket] (login-provider)]
      (let [response (crdsa-login/login
                       {:username             (.getUser userdetails)
                        :ticket               ticket
                        :success-redirect-url redirect-url})]
        (login-succeeded response userdetails))
      (login-failed config))
    (catch Throwable e
      (login-failed config e))))

(defn cas-initiated-logout [logout-request session-store]
  (log/info "cas-initiated logout")
  (let [cas-logout (CasLogout.)
        ticket (.parseTicketFromLogoutRequest cas-logout logout-request)]
    (log/info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (log/error "Could not parse ticket from CAS request" logout-request)
      (crdsa-session-store/logout-by-ticket! session-store (.get ticket)))
    (ok)))
