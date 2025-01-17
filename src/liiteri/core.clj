(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.audit-log :as audit-log]
            [liiteri.config :as config]
            [liiteri.auth.cas-client :as cas]
            [liiteri.db :as db]
            [liiteri.files.s3-client :as s3-client]
            [liiteri.files.s3-store :as s3-store]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server]
            [liiteri.server_background :as server_background]
            [liiteri.virus-scan :as virus-scan]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [liiteri.file-cleaner :as file-cleaner]
            [liiteri.mime-fixer :as mime-fixer]
            [liiteri.preview.preview-generator :as preview-generator]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.community.rolling :refer [rolling-appender]]
            [timbre-ns-pattern-level :as pattern-level]
            [liiteri.local :as local]
            [environ.core :refer [env]])
  (:import [java.util TimeZone])
  (:gen-class))

(defn new-system [is_background & [config-overrides]]
  (let [config (config/new-config config-overrides)]
    (log/merge-config! {:timestamp-opts {:pattern  "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                                         :timezone (TimeZone/getTimeZone "Europe/Helsinki")}
                        :appenders      {:standard-out     {:enabled? false}
                                         :println          nil
                                         :file-appender   (rolling-appender
                                                            {:path
                                                             (get-in config [:app-log :path]
                                                                     (if is_background "/tmp/app_liiteri-background.log" "/tmp/app_liiteri.log"))
                                                             :pattern :daily})}
                        :middleware     [(pattern-level/middleware {"com.zaxxer.hikari.HikariConfig" :debug
                                                                    :all                             :info})]
                        :output-fn      (partial log/default-output-fn {:stacktrace-fonts {}})})
    (if is_background
      (component/system-map
        :config config

        :db (component/using
              (db/new-pool)
              [:config])

        :migrations (component/using
                      (migrations/new-migration)
                      [:db])

        :server (component/using
                  (server_background/new-server) [:config])

        :file-cleaner (component/using
                        (file-cleaner/new-cleaner false)
                        [:db :storage-engine :config :migrations])

        :deleted-file-cleaner (component/using
                                (file-cleaner/new-cleaner true)
                                [:db :storage-engine :config :migrations])

        :mime-fixer (component/using
                      (mime-fixer/new-mime-fixer)
                      [:db :storage-engine :config :migrations])

        :preview-generator (component/using
                             (preview-generator/new-preview-generator)
                             [:db :storage-engine :config :migrations])

        :s3-client (component/using
                     (s3-client/new-client)
                     [:config])

        :storage-engine (component/using
                          (s3-store/new-store)
                          [:s3-client :config])

        :local (component/using (local/new-local) [:config :s3-client]))
      (component/system-map
        :config config

        :login-cas-client (delay (cas/new-cas-client config))

        :kayttooikeus-cas-client (delay (cas/new-client config
                                                        "/kayttooikeus-service" "j_spring_cas_security_check"
                                                        "JSESSIONID"))

        :audit-logger (component/using
                        (audit-log/new-logger)
                        [:config])

        :db (component/using
              (db/new-pool)
              [:config])

        :session-store (create-session-store (db/get-datasource config))

        :server (component/using
                  (server/new-server)
                  [:storage-engine :login-cas-client :kayttooikeus-cas-client :session-store :db :config :audit-logger :virus-scan])

        :migrations (component/using
                      (migrations/new-migration)
                      [:db])

        :virus-scan (component/using
                      (virus-scan/new-scanner)
                      [:db :storage-engine :config :migrations])

        :s3-client (component/using
                     (s3-client/new-client)
                     [:config])

        :storage-engine (component/using
                          (s3-store/new-store)
                          [:s3-client :config])

        :local (component/using (local/new-local) [:config :s3-client])))
))

(defn -main [& _]
  (let [_ (component/start-system (new-system (= "liiteri-background" (:app env))))]
    @(promise)))
