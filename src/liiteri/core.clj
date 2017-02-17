(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server])
  (:gen-class))

(defn new-system []
  (component/system-map
    :config     (config/new-config)

    :db         (component/using
                  (db/new-pool)
                  [:config])

    :server     (component/using
                  (server/new-server)
                  [:db])

    :migrations (component/using
                  (migrations/new-migration)
                  [:db])))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
