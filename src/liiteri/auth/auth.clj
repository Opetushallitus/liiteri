(ns liiteri.auth.auth
  (:require [clj-ring-db-session.authentication.login :as crdsa-login]
            [clj-ring-db-session.session.session-store :as crdsa-session-store]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [taoensso.timbre :as log])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(defn- redirect-to-login-failed-page [config]
  (resp/redirect (str (-> config :virkailija-host) "/liiteri/virhe")))

(defn cas-login [config cas-client ticket]
  (fn []
    (when ticket
      [(.run (.validateServiceTicket cas-client (str (-> config :virkailija-host) "/liiteri/auth/cas") ticket))
       ticket])))

(defn- login-failed
  ([config e]
   (log/error e "Error in login ticket handling")
   (redirect-to-login-failed-page config))
  ([config]
   (redirect-to-login-failed-page config)))

(defn- login-succeeded [response username]
  (log/info "user" username "logged in")
  (update-in
    response
    [:session :identity]
    assoc
    :superuser true))

(defn login [login-provider
             redirect-url
             config]
  (try
    (if-let [[username ticket] (login-provider)]
      (let [response   (crdsa-login/login
                         {:username             username
                          :ticket               ticket
                          :success-redirect-url redirect-url})]
        (login-succeeded response username))
      (login-failed config))
    (catch Exception e
      (login-failed config e))))

(defn cas-initiated-logout [logout-request session-store]
  (log/info "cas-initiated logout")
  (let [ticket (CasLogout/parseTicketFromLogoutRequest logout-request)]
    (log/info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (log/error "Could not parse ticket from CAS request" logout-request)
      (crdsa-session-store/logout-by-ticket! session-store (.get ticket)))
    (ok)))
