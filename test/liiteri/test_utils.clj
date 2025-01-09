(ns liiteri.test-utils
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [liiteri.files.file-store :as file-store]))

(defn start-system [system]
  (let [s @system]
    (reset! system (component/start-system s))))

(defn stop-system [system]
  (component/stop-system @system))

(defn clear-database! [system]
  (let [datasource (-> (:db @system)
                       (select-keys [:datasource]))]
    (jdbc/db-do-commands datasource ["DROP SCHEMA IF EXISTS public CASCADE"
                                     "CREATE SCHEMA public"])))

(def files-atom (atom {}))

(defrecord InMemoryS3Store [config]
  file-store/StorageEngine

  (create-file [this file file-key]
    (swap! files-atom assoc file-key file))

  (create-file-from-bytearray [this file-bytes file-key]
    (swap! files-atom assoc file-key file-bytes))

  (delete-file [this file-key]
    (swap! files-atom dissoc file-key))

  (get-file [this file-key]
    (io/input-stream (get @files-atom file-key))))

(defn new-in-memory-store []
  (map->InMemoryS3Store {}))
