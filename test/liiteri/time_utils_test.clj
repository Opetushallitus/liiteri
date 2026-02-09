(ns liiteri.time-utils-test
  (:require [clojure.test :refer [deftest is testing]]
            [liiteri.time-utils :as time-utils])
  (:import (java.sql Date Timestamp)
           (java.time LocalDate Instant ZonedDateTime)))

(defonce exact-time 1770112704312)

(defn- breakdown
  "Palauttaa ajan pilkottuna osiin: [vuosi kk pv t m s milli vyöhyke]"
  [^ZonedDateTime time]
  [(.getYear time)
   (.getMonthValue time)
   (.getDayOfMonth time)
   (.getHour time)
   (.getMinute time)
   (.getSecond time)
   (-> time .getNano (/ 1000000) int)
   (-> time .getZone .getId)])

(defn- parse-test-sql-time ^Timestamp [date-str]
  (Timestamp/from (Instant/parse date-str)))

(defn- parse-test-sql-date ^Date [date-str]
  (Date/valueOf (LocalDate/parse date-str)))

(deftest date-time->sql-time
  (testing "returns correct timestamp"
    (is (= exact-time (.getTime (time-utils/date-time->sql-time (ZonedDateTime/parse "2026-02-03T11:58:24.312+02:00")))))))

(deftest sql-time->date-time
  (testing "returns correct date time when given a timestamp"
    (is (= [2025 1 1 17 42 31 0 "Europe/Helsinki"] (breakdown (time-utils/sql-time->date-time (parse-test-sql-time "2025-01-01T15:42:31Z")))))
    (is (= [2025 1 1 17 42 31 134 "Europe/Helsinki"] (breakdown (time-utils/sql-time->date-time (parse-test-sql-time "2025-01-01T15:42:31.134Z"))))))
  (testing "returns back the parameter when given an SQL date"
    (let [param (parse-test-sql-date "2025-01-01")]
      (is (= param (time-utils/sql-time->date-time param)))))
  (testing "returns nil when given nil"
    (is (= nil (time-utils/sql-time->date-time nil)))))

(deftest sql-date->date-time
  (testing "returns the midnight timestamp when given an SQL date"
    (is (= [2025 01 01 00 0 0 0 "Europe/Helsinki"] (breakdown (time-utils/sql-date->date-time (parse-test-sql-date "2025-01-01")))))
    (is (= [2024 12 31 00 0 0 0 "Europe/Helsinki"] (breakdown (time-utils/sql-date->date-time (parse-test-sql-date "2024-12-31")))))))

(deftest before?
  (let [first (ZonedDateTime/parse "2024-12-31T13:00:00Z")
        second (ZonedDateTime/parse "2025-01-01T13:00:00Z")]
    (testing "returns true only when this is strictly before that"
      (is (= true (time-utils/before? first second)))
      (is (= false (time-utils/before? second first)))
      (is (= false (time-utils/before? first first))))))

(deftest periodic-seq
  (with-redefs [time-utils/current-time-millis (constantly exact-time)]
    (testing "starts at the current time"
      (is (= exact-time (first (time-utils/periodic-seq 60)))))
    (testing "increases with seconds with each element"
      (is (= (+ exact-time 60000) (nth (time-utils/periodic-seq 60) 1)))
      (is (= (+ exact-time 120000) (nth (time-utils/periodic-seq 60) 2)))
      (is (= (+ exact-time 180000) (nth (time-utils/periodic-seq 60) 3))))))
