(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.db :as db]
            [liiteri.server :as server])
  (:gen-class))

(defn new-system []
  (component/system-map
    :db     (db/new-pool)
    :server (server/new-server)))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
