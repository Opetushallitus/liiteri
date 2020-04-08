(ns liiteri.migrations
  (:require [com.stuartsierra.component :as component])
  (:import [org.flywaydb.core Flyway]))

(defrecord Migration []
  component/Lifecycle

  (start [{:keys [db] :as this}]
    (let [config (doto (Flyway/configure)
                   (.dataSource (:datasource db))
                   (.table "schema_version")
                   (.schemas (into-array String ["public"]))
                   (.locations (into-array String ["migrations"])))
          flyway (.load config)]
      (.migrate flyway))
    this)

  (stop [this]
    this))

(defn new-migration []
  (->Migration))
