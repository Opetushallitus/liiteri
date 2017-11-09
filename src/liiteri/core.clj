(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.audit-log :as audit-log]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.files.filesystem-store :as filesystem-store]
            [liiteri.files.s3-client :as s3-client]
            [liiteri.files.s3-store :as s3-store]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server]
            [liiteri.virus-scan :as virus-scan]
            [liiteri.file-cleaner :as file-cleaner]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import [java.util TimeZone])
  (:gen-class))

(defn new-system [& [config-overrides]]
  (log/merge-config! {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss ZZ"
                                       :timezone (TimeZone/getTimeZone "Europe/Helsinki")}})
  (let [config (config/new-config config-overrides)]
    (component/system-map :config         config

                          :audit-logger   (audit-log/new-logger)

                          :db             (component/using
                                           (db/new-pool)
                                           [:config])

                          :server         (component/using
                                           (server/new-server)
                                           [:storage-engine :db :config :audit-logger])

                          :migrations     (component/using
                                           (migrations/new-migration)
                                           [:db])

                          :virus-scan     (component/using
                                           (virus-scan/new-scanner)
                                           [:db :storage-engine :config :migrations])

                          :file-cleaner   (component/using
                                           (file-cleaner/new-cleaner)
                                           [:db :storage-engine :config :migrations])

                          :s3-client      (component/using
                                           (s3-client/new-client)
                                           [:config])

                          :storage-engine (case (get-in config [:file-store :engine])
                                            :filesystem (component/using
                                                         (filesystem-store/new-store)
                                                         [:config])
                                            :s3         (component/using
                                                         (s3-store/new-store)
                                                         [:s3-client :config])))))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
