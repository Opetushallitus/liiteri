(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.server :as server])
  (:gen-class))

(defn new-system []
  (component/system-map
    :config (config/new-config)

    :db     (component/using
              (db/new-pool)
              [:config])

    :server (server/new-server)))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
