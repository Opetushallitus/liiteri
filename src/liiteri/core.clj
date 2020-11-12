(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.audit-log :as audit-log]
            [liiteri.config :as config]
            [liiteri.auth.cas-client :as cas]
            [liiteri.db :as db]
            [liiteri.files.filesystem-store :as filesystem-store]
            [liiteri.files.s3-client :as s3-client]
            [liiteri.files.s3-store :as s3-store]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server]
            [liiteri.virus-scan :as virus-scan]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [liiteri.file-cleaner :as file-cleaner]
            [liiteri.mime-fixer :as mime-fixer]
            [liiteri.preview.preview-generator :as preview-generator]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders])
  (:import [java.util TimeZone])
  (:gen-class))

(defn new-system [& [config-overrides]]
  (log/merge-config! {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss ZZ"
                                       :timezone (TimeZone/getTimeZone "Europe/Helsinki")}
                      :appenders
                                      {:println
                                       {:min-level    :info
                                        :ns-blacklist ["com.zaxxer.hikari.HikariConfig"]}
                                       :debug-level-println
                                       (assoc (appenders/println-appender)
                                         :min-level :debug
                                         :ns-whitelist ["com.zaxxer.hikari.HikariConfig"])}
                      :output-fn      (partial log/default-output-fn {:stacktrace-fonts {}})})
  (let [config (config/new-config config-overrides)]
    (apply component/system-map
           :config config

           :login-cas-client (delay (cas/new-cas-client config))

           :audit-logger (component/using
                           (audit-log/new-logger)
                           [:config])

           :db (component/using
                 (db/new-pool)
                 [:config])

           :session-store (create-session-store (db/get-datasource config))

           :server (component/using
                     (server/new-server)
                     [:storage-engine :login-cas-client :session-store :db :config :audit-logger])

           :migrations (component/using
                         (migrations/new-migration)
                         [:db])

           :virus-scan (component/using
                         (virus-scan/new-scanner)
                         [:db :storage-engine :config :migrations])

           :file-cleaner (component/using
                           (file-cleaner/new-cleaner)
                           [:db :storage-engine :config :migrations])

           :mime-fixer (component/using
                         (mime-fixer/new-mime-fixer)
                         [:db :storage-engine :config :migrations])

           :preview-generator (component/using
                                (preview-generator/new-preview-generator)
                                [:db :storage-engine :config :migrations])

           (case (get-in config [:file-store :engine])
             :filesystem [:storage-engine (component/using
                                            (filesystem-store/new-store)
                                            [:config])]
             :s3 [:s3-client (component/using
                               (s3-client/new-client)
                               [:config])
                  :storage-engine (component/using
                                    (s3-store/new-store)
                                    [:s3-client :config])]))))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
