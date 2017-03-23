(ns liiteri.files.filesystem-store
  (:require [liiteri.files.file-store :as file-store]))

(defrecord FilesystemStore [config]
  file-store/StorageEngine

  (create-file [this file file-key]
    (println (str "create-file")))

  (update-file [this file file-key]
    (println (str "update-efile")))

  (delete-file [this file-key]
    (println (str "delete-file")))

  (get-file [this file-key]
    (println (str "get-file"))))

(defn new-store []
  (map->FilesystemStore {}))
