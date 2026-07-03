(ns liiteri.db.db-utils
  (:require [camel-snake-kebab.core :as t]
            [camel-snake-kebab.extras :as e]
            [liiteri.time-utils :as time-utils]
            [clojure.walk]))

(defn kwd->snake-case [data]
  {:pre [(map? data)]}
  (e/transform-keys t/->snake_case_keyword data))

(defn kwd->kebab-case [data]
  {:pre [(map? data)]}
  (e/transform-keys t/->kebab-case-keyword data))

(defn- transform-values [data t]
  (clojure.walk/prewalk (fn [x]
                          (cond->> x
                                   (map? x)
                                   (into {} (map (fn [[k v]] [k (t v)])))))
                        data))

(defn unwrap-data [data]
  (some-> data
          kwd->kebab-case
          (transform-values time-utils/sql-time->date-time)))
