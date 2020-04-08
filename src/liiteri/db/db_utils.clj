(ns liiteri.db.db-utils
  (:require [camel-snake-kebab.core :as t]
            [camel-snake-kebab.extras :as e]
            [clj-time.coerce :as c])
  (:import [java.sql Timestamp]))

(defn kwd->snake-case [data]
  {:pre [(map? data)]}
  (e/transform-keys t/->snake_case_keyword data))

(defn kwd->kebab-case [data]
  {:pre [(map? data)]}
  (e/transform-keys t/->kebab-case-keyword data))

(defn- sql-date->joda-time [x]
  (cond-> x
          (instance? Timestamp x)
          (c/from-sql-date)))

(defn- transform-values [data t]
  (clojure.walk/prewalk (fn [x]
                          (cond->> x
                                   (map? x)
                                   (into {} (map (fn [[k v]] [k (t v)])))))
                        data))

(defn unwrap-data [data]
  (some-> data
          kwd->kebab-case
          (transform-values sql-date->joda-time)))
