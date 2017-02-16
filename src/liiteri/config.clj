(ns liiteri.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]))

(defn new-config []
  (let [path (System/getProperty "config" "dev-resources/dev-config.edn")]
    (-> path
        slurp
        edn/read-string)))
