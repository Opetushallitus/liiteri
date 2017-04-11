(ns liiteri.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]))

(defn new-config [& [overrides]]
  {:pre [(not (clojure.string/blank? (env :config)))]}
  (let [path (env :config)]
    (-> path
        slurp
        edn/read-string
        (merge overrides))))
