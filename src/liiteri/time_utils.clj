(ns liiteri.time-utils
  (:require [cheshire.factory]
            [cheshire.generate :as cheshire])
  (:import (java.sql Timestamp)
           (java.time Instant ZoneId ZonedDateTime)
           (java.time.format DateTimeFormatter)))

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

(defonce timezone-utc (ZoneId/of "UTC"))
(defn formatter-for-utc [fmt-str] (-> (DateTimeFormatter/ofPattern fmt-str) (.withZone timezone-utc)))
(defonce formatter-utc-with-millis (formatter-for-utc "yyyy-MM-dd'T'HH:mm:ss.SSSX"))

(cheshire/add-encoder ZonedDateTime
                      (fn [c jsonGenerator]
                        (.writeString jsonGenerator (.format formatter-utc-with-millis c))))
