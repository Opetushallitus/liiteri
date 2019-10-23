(ns liiteri.test-utils
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [liiteri.core :as system]
            [clojure.java.io :as io]
            [liiteri.files.file-store :as file-store]))

(defn start-system [system]
  (let [s (or @system (system/new-system))]
    (reset! system (component/start-system s))))

(defn stop-system [system]
  (component/stop-system @system))

(defn clear-database! [system]
  (let [datasource (-> (:db @system)
                       (select-keys [:datasource]))]
    (jdbc/db-do-commands datasource ["DROP SCHEMA IF EXISTS public CASCADE"
                                     "CREATE SCHEMA public"])))

(defn- temp-dir [system]
  (-> (get-in (:config @system) [:file-store :filesystem :base-path])
      (io/file)))

(defn create-temp-dir [system]
  (.mkdirs (temp-dir system)))

(defn remove-temp-dir [system]
  (letfn [(remove-node [node]
            (when (.isDirectory node)
              (doseq [child-node (.listFiles node)]
                (remove-node child-node)))
            (io/delete-file node))]
    (remove-node (temp-dir system))))

(def files-atom (atom {}))

(defrecord InMemoryS3Store [config]
  file-store/StorageEngine

  (create-file [this file file-key]
    (swap! files-atom assoc file-key file))

  (delete-file [this file-key]
    (swap! files-atom dissoc file-key))

  (get-file [this file-key]
    (io/input-stream (get @files-atom file-key))))

(defn new-in-memory-store []
  (map->InMemoryS3Store {}))
