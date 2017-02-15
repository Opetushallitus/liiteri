(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.server :as server])
  (:gen-class))

(defn new-system []
  (let [components [:server (server/new-server)]]
    (apply component/system-map components)))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
