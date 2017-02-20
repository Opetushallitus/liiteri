(ns liiteri.db.file-store
  (:require [liiteri.db.db-utils :as db-utils]
            [liiteri.utils :as utils]
            [yesql.core :as sql]))

(sql/defqueries "sql/files.sql")

;; conn = datasource wrapped inside a DB transaction
;; db   = a datasource, auto-commit

(defn create-file [spec conn]
  {:pre [(utils/not-blank? (:id spec))
         (utils/not-blank? (:filename spec))
         (utils/not-blank? (:content-type spec))]}
  (-> (db-utils/kwd->snake-case spec)
      (sql-create-file<! conn)
      (db-utils/unwrap-data)))

(defn delete-file [id db]
  {:pre [(utils/not-blank? id)]}
  (let [conn {:connection db}]
    (-> (sql-delete-file! {:file_id id} conn))))

(defn create-version [version file-id conn]
  {:pre [(utils/not-blank? version)
         (utils/not-blank? file-id)]}
  (-> (sql-create-version<! {:file_id file-id :version version} conn)
      (db-utils/unwrap-data)))

(defn get-file-for-update [id conn]
  {:pre [(utils/not-blank? id)]}
  (->> (sql-get-file-for-update {:id id} conn)
       (map db-utils/unwrap-data)))
