(ns liiteri.time-utils
  (:import (java.sql Timestamp)
           (java.time Instant ZoneId ZonedDateTime)))

(defonce timezone-fi (ZoneId/of "Europe/Helsinki"))

(defn current-time-millis []
  (.toEpochMilli (Instant/now)))

(defn date-time->sql-time ^Timestamp [^ZonedDateTime date-time]
  (Timestamp/from (.toInstant date-time)))

(defn sql-time->date-time ^ZonedDateTime [x]
  (cond-> x
          (instance? Timestamp x)
          (-> (.toInstant) (ZonedDateTime/ofInstant timezone-fi))))

(defn sql-date->date-time ^ZonedDateTime [x]
  (-> x (.toLocalDate) (.atStartOfDay timezone-fi)))

(defn before? [^ZonedDateTime this that]
  (.isBefore this that))

(defn periodic-seq
  "Päättymätön sarja aikaleimoja alkaen nykyhetkestä aina interval-seconds sekunnin välein, millisekunteina"
  [interval-seconds]
  (let [a (* interval-seconds 1000)]
    (iterate (partial + a) (current-time-millis))))
