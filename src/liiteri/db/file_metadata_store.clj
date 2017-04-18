(ns liiteri.db.file-metadata-store
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
    (sql-delete-file! {:key key} conn)))

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
         (map second)
         (first))))

(defn get-unscanned-file [conn]
  (->> (sql-get-unscanned-file {} conn)
       (map db-utils/unwrap-data)
       (first)))

(defn set-virus-scan-status! [file-key status conn]
  (sql-set-virus-scan-status! {:file_key          file-key
                               :virus_scan_status (name status)}
                              conn))
