(ns liiteri.db.file-store
  (:require [liiteri.db.db-utils :as db-utils]
            [yesql.core :as sql]))

(sql/defqueries "sql/files.sql")

(defn- not-blank? [string]
  (not (clojure.string/blank? string)))

(defn create-file [spec conn]
  {:pre [(not-blank? (:id spec))
         (not-blank? (:filename spec))
         (not-blank? (:content-type spec))]}
  (-> (db-utils/kwd->snake-case spec)
      (sql-create-file<! conn)
      (db-utils/unwrap-data)))

(defn delete-file [id db]
  {:pre [(not-blank? id)]}
  (let [conn {:connection db}]
    (-> (sql-delete-file! {:file_id id} conn))))

(defn create-version [version file-id conn]
  {:pre [(not-blank? version)
         (not-blank? file-id)]}
  (-> (sql-create-version<! {:file_id file-id :version version} conn)
      (db-utils/unwrap-data)))
