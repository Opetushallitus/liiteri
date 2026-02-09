(ns liiteri.time-utils
  (:require [clj-time.coerce :as coerce]
            [clj-time.core :as time])
  (:import (java.sql Timestamp)
           (org.joda.time DateTime Instant)))

(defn current-time-millis []
  (.getMillis (Instant/now)))

(defn date-time->sql-time ^Timestamp [date-time]
  (coerce/to-sql-time date-time))

(defn sql-time->joda-time ^DateTime [x]
  (cond-> x
          (instance? Timestamp x)
          (coerce/from-sql-date)))

(defn sql-date->joda-time ^DateTime [x]
  (coerce/from-sql-date x))

(defn before? [this that]
  (time/before? this that))

(defn periodic-seq
  "Päättymätön sarja aikaleimoja alkaen nykyhetkestä aina interval-seconds sekunnin välein, millisekunteina"
  [interval-seconds]
  (let [a (* interval-seconds 1000)]
    (iterate (partial + a) (current-time-millis))))
