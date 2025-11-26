(ns liiteri.urls)

(defn redirect-to-login-failed-page-url [config]
  (str (-> config :virkailija-host) "/liiteri/virhe"))

(defn liiteri-login-url [config]
  (str (-> config :virkailija-host) "/liiteri/auth/cas"))

(defn cas-login-url [config]
  (let [host (-> config :virkailija-host)]
    (str host "/cas/login?service=" host "/liiteri/auth/cas")))
(defn cas-logout-url [config]
  (let [host (-> config :virkailija-host)]
    (str host "/cas/logout?service=" host "/liiteri/auth/cas")))

(defn cas-redirect-url [config]
  (str (-> config :virkailija-host) "/liiteri/auth/checkpermission"))
