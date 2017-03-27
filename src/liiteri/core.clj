(ns liiteri.core
  (:require [com.stuartsierra.component :as component]
            [liiteri.config :as config]
            [liiteri.db :as db]
            [liiteri.migrations :as migrations]
            [liiteri.server :as server]
            [liiteri.files.filesystem-store :as filesystem-store]
            [liiteri.files.s3-client :as s3-client]
            [liiteri.files.s3-store :as s3-store]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import [java.util TimeZone])
  (:gen-class))

(defn new-system []
  (log/merge-config! {:timestamp-opts {:pattern  "yyyy-MM-dd HH:mm:ss ZZ"
                                       :timezone (TimeZone/getTimeZone "Europe/Helsinki")}})
  (let [config          (config/new-config)
        base-components [:config     config

                         :db         (component/using
                                       (db/new-pool)
                                       [:config])

                         :server     (component/using
                                       (server/new-server)
                                       [:storage-engine :db :config])

                         :migrations (component/using
                                       (migrations/new-migration)
                                       [:db])]
        file-components (case (get-in config [:file-store :engine])
                          :s3 [:s3-client      (s3-client/new-client)

                               :storage-engine (component/using
                                                 (s3-store/new-store)
                                                 [:s3-client :db])]
                          :filesystem [:storage-engine (component/using
                                                         (filesystem-store/new-store)
                                                         [:config])])]
    (apply component/system-map (concat base-components
                                        file-components))))

(defn -main [& _]
  (let [_ (component/start-system (new-system))]
    @(promise)))
