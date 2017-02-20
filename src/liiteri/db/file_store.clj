(ns liiteri.db.file-store
  (:require [liiteri.db.db-utils :as db-utils]
            [liiteri.schema :as schema]
            [schema.core :as s]
            [schema-tools.core :as st]
            [yesql.core :as sql])
  (:import [org.joda.time DateTime]))

(sql/defqueries "sql/files.sql")

;; conn = datasource wrapped inside a DB transaction
;; db   = a datasource, auto-commit

(s/defn create-file :- schema/File
  [spec :- schema/File
   conn :- s/Any]
  (-> (db-utils/kwd->snake-case spec)
      (sql-create-file<! conn)
      (db-utils/unwrap-data)))

(s/defn delete-file :- s/Int
  [id :- s/Str
   db :- s/Any]
  (let [conn {:connection db}]
    (-> (sql-delete-file! {:file_id id} conn))))

(s/defn create-version :- schema/Version
  [version :- s/Str
   file-id :- s/Str
   conn :- s/Any]
  (-> (sql-create-version<! {:file_id file-id :version version} conn)
      (db-utils/unwrap-data)))

(s/defn get-file-for-update :- [(assoc (st/select-keys schema/Version [:version :uploaded :deleted]) :id s/Str)]
  [id :- s/Str
   conn :- s/Any]
  (->> (sql-get-file-for-update {:id id} conn)
       (map db-utils/unwrap-data)))
