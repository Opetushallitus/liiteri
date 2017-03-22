(ns liiteri.db.file-store
  (:require [clj-time.core :as t]
            [liiteri.db.db-utils :as db-utils]
            [liiteri.schema :as schema]
            [schema-tools.core :as st]
            [yesql.core :as sql])
  (:import [org.joda.time DateTime]))

(sql/defqueries "sql/files.sql")

;; conn = datasource wrapped inside a DB transaction
;; db   = a datasource, auto-commit

(defn create-file [spec conn]
  (-> (db-utils/kwd->snake-case spec)
      (sql-create-file<! conn)
      (db-utils/unwrap-data)
      (dissoc :id)))

(defn delete-file [key db]
  (let [conn {:connection db}]
    (-> (sql-delete-file! {:key key} conn))))

(defn get-file-for-update [key conn]
  (->> (sql-get-file-for-update {:key key} conn)
       (map db-utils/unwrap-data)))

(defn get-metadata [key-list db]
  (let [conn {:connection db}]
    (->> (sql-get-metadata {:keys key-list} conn)
         (map db-utils/unwrap-data)
         (reduce (fn pick-latest-metadata [result {:keys [key uploaded] :as metadata}]
                   (cond-> result
                     (or (not (contains? result key))
                         (t/before? (get-in result [key :uploaded]) uploaded))
                     (assoc key metadata)))
                 {})
         (map second))))

(s/defn get-unchecked-files :- [schema/File]
  [db :- s/Any]
  (let [conn {:connection db}]
    (->> (sql-get-non-virus-checked {} conn)
         (map db-utils/unwrap-data))))

(s/defn mark-virus-checked
  [key :- s/Str
   db :- s/Any]
  (let [conn {:connection db}]
    (-> (sql-mark-virus-checked! {:key key} conn))))