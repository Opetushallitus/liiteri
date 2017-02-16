(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.server :as server])
  (:gen-class))

(defn new-system []
  (component/system-map
    :server (server/new-server)))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
