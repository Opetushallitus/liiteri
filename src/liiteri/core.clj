(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server]
            [liiteri.files.s3.s3-client :as s3-client]
            [schema.core :as s])
  (:gen-class))

(defn new-system []
  (component/system-map
    :config     (config/new-config)

    :s3-client  (s3-client/new-client)

    :db         (component/using
                  (db/new-pool)
                  [:config])

    :server     (component/using
                  (server/new-server)
                  [:db :s3-client])

    :migrations (component/using
                  (migrations/new-migration)
                  [:db])))

(defn -main [& _]
  (s/set-fn-validation! true)
  (let [_ (component/start-system (new-system))]
    @(promise)))
