(ns liiteri.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def config (-> "dev-config.edn"
                io/resource
                slurp
                edn/read-string))
