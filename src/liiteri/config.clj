(ns liiteri.config
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [environ.core :refer [env]]))

(defn new-config [& [overrides]]
  {:pre [(not (string/blank? (env :config)))]}
  (let [path (env :config)]
    (-> path
        slurp
        edn/read-string
        (merge overrides))))
