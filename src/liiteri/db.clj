(ns liiteri.db
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :as h])
  (:import [org.postgresql.util PGobject]))

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

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _ _]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "jsonb" (json/parse-string value true)
        :else value))))

(defrecord DbPool []
  component/Lifecycle

  (start [{:keys [config] :as this}]
    (assoc this :datasource (get-datasource config)))

  (stop [this]
    (when-let [datasource (:datasource this)]
      (h/close-datasource datasource))
    (assoc this :datasource nil)))

(defn new-pool []
  (->DbPool))
