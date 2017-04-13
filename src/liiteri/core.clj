(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.audit-log :as audit-log]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.files.filesystem-store :as filesystem-store]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server]
            [liiteri.virus-scan :as virus-scan]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import [java.util TimeZone])
  (:gen-class))

(defn new-system [& [config-overrides]]
  (s/set-fn-validation! true)
  (log/merge-config! {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss ZZ"
                                       :timezone (TimeZone/getTimeZone "Europe/Helsinki")}})
  (component/system-map :config         (config/new-config config-overrides)

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

                        :storage-engine (component/using
                                          (filesystem-store/new-store)
                                          [:config])))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
