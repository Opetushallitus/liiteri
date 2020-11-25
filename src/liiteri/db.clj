(ns liiteri.db
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clj-time.coerce :as c]
            [hikari-cp.core :as h])
  (:import (java.sql PreparedStatement)
           (java.sql Date)
           (java.sql Timestamp)
           (org.joda.time DateTime)
           (org.postgresql.util PGobject)
           (org.postgresql.jdbc PgArray)))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentCollection
  (sql-value [value]
    (doto (PGobject.)
      (.setType "jsonb")
      (.setValue (json/generate-string value)))))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _ _]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (json/parse-string value true)
        "jsonb" (json/parse-string value true)
        :else value))))

(extend-protocol jdbc/IResultSetReadColumn
  Date
  (result-set-read-column [v _ _] (c/from-sql-date v))

  Timestamp
  (result-set-read-column [v _ _] (c/from-sql-time v))

  PgArray
  (result-set-read-column [v _ _]
    (vec (.getArray v))))

(extend-type DateTime
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (c/to-sql-time v))))

(defonce datasource (atom nil))

(defn get-datasource [config]
  (swap! datasource (fn [db?]
                      (if db?
                        db?
                        (let [db-config (merge {:auto-commit        true
                                                :read-only          false
                                                :connection-timeout 30000
                                                :validation-timeout 5000
                                                :idle-timeout       600000
                                                :max-lifetime       1800000
                                                :register-mbeans    false
                                                :adapter            "postgresql"}
                                               (:db config))]
                          (h/make-datasource db-config)))))
  @datasource)

(defrecord DbPool []
  component/Lifecycle

  (start [{:keys [config] :as this}]
    (assoc this :datasource (get-datasource config)))

  (stop [this]
    (when-let [db (:datasource this)]
      (reset! datasource nil)
      (h/close-datasource db))
    (assoc this :datasource nil)))

(defn new-pool []
  (->DbPool))
