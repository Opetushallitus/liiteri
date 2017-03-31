(ns liiteri.files.filesystem-store
  (:require [liiteri.files.file-store :as file-store]
            [clojure.java.io :as io]))

(defrecord FilesystemStore [config]
  file-store/StorageEngine

  (create-file [this temp-file file-key]
    (let [base-path (get-in config [:file-store :filesystem :base-path])
          dir       (io/file base-path)
          dest-file (io/file (str base-path "/" file-key))]
      (.mkdirs dir)
      (io/copy temp-file dest-file)))

  (delete-file [this file-key]
    (let [base-path (get-in config [:file-store :filesystem :base-path])
          file      (io/file (str base-path "/" file-key))]
      (io/delete-file file true)))

  (get-file [this file-key]
    (let [base-path (get-in config [:file-store :filesystem :base-path])
          path     (str base-path "/" file-key)]
      (io/file path))))

(defn new-store []
  (map->FilesystemStore {}))
