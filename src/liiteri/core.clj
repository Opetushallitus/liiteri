(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server]
            [liiteri.s3-client :as s3-client]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import [java.util TimeZone])
  (:gen-class))

(defn new-system []
  (log/merge-config! {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss ZZ"
                                       :timezone (TimeZone/getTimeZone "Europe/Helsinki")}})
  (component/system-map
    :config     (config/new-config)

    :s3-client  (s3-client/new-client)

    :db         (component/using
                  (db/new-pool)
                  [:config])

    :server     (component/using
                  (server/new-server)
                  [:db :s3-client])

    :migrations (component/using
                  (migrations/new-migration)
                  [:db])))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
