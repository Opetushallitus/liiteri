(ns liiteri.db-schema-diagram
  (:gen-class)
  (:require [clojure.java.shell :as shell]
            [com.stuartsierra.component :as component]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.migrations :as migrations]))

(defn- generate-db-schema-diagram [config]
  (let [version (-> "./project.clj" slurp read-string (nth 2))
        {:keys [server-name port-number database-name username password]} (:db config)
        ret     (shell/sh "./bin/generate-db-schema-diagram.sh"
                          server-name
                          (str port-number)
                          database-name
                          "./target/db-schema"
                          version
                          username
                          password)]
    (clojure.pprint/pprint ret)
    (:exit ret)))

(defn -main [& _]
  (let [system-map (component/system-map :config     (config/new-config)

                                         :db         (component/using
                                                       (db/new-pool)
                                                       [:config])

                                         :migrations (component/using
                                                       (migrations/new-migration)
                                                       [:db]))
        system     (component/start-system system-map)]
    (generate-db-schema-diagram (:config system))
    (component/stop-system system)))
