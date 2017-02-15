(ns liiteri.core
  (:require [com.stuartsierra.component :as component])
  (:gen-class))

(defn new-system []
  (let [components {}]
    (apply component/system-map components)))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
