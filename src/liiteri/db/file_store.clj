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
      (db-utils/unwrap-data)
      (dissoc :id)))

(s/defn delete-file :- s/Int
  [key :- s/Str
   db :- s/Any]
  (let [conn {:connection db}]
    (-> (sql-delete-file! {:key key} conn))))

(s/defn get-file-for-update :- [{:deleted (s/maybe DateTime)}]
  [key :- s/Str
   conn :- s/Any]
  (->> (sql-get-file-for-update {:key key} conn)
       (map db-utils/unwrap-data)))
