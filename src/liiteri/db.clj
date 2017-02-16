(ns liiteri.db
  (:require [com.stuartsierra.component :as component]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :as h])
  (:import [org.postgresql.util PGobject]))

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
    (let [db-config  (merge {:auto-commit        true
                             :read-only          false
                             :connection-timeout 30000
                             :validation-timeout 5000
                             :idle-timeout       600000
                             :max-lifetime       1800000
                             :register-mbeans    false
                             :adapter            "postgresql"}
                            (:db config))
          datasource (h/make-datasource db-config)]
      (assoc this :datasource datasource)))

  (stop [this]
    (when-let [datasource (:datasource this)]
      (h/close-datasource datasource))
    (assoc this :datasource nil)))

(defn new-pool []
  (->DbPool))
