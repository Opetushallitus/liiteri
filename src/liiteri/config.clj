(ns liiteri.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]))

(defn new-config []
  (-> "dev-config.edn"
      io/resource
      slurp
      edn/read-string))
