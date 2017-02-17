(ns liiteri.migrations
  (:require [com.stuartsierra.component :as component])
  (:import [org.flywaydb.core Flyway]))

(defrecord Migration []
  component/Lifecycle

  (start [{:keys [db] :as this}]
    (let [flyway (doto (Flyway.)
                   (.setSchemas (into-array String ["public"]))
                   (.setDataSource (:datasource db))
                   (.setLocations (into-array String ["migrations"])))]
      (.migrate flyway)
      this))

  (stop [this]
    this))

(defn new-migration []
  (->Migration))
