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

(defmacro with-db [bindings & body]
  `(let [conn-arg# ~(second bindings)
         ~(first bindings) (if (and (map? conn-arg#)
                                    (contains? conn-arg# :connection))
                             conn-arg#
                             {:connection conn-arg#})]
     ~@body))

(defn create-file [spec db]
  (with-db [conn db]
    (-> (db-utils/kwd->snake-case spec)
        (sql-create-file<! conn)
        (db-utils/unwrap-data)
        (dissoc :id))))

(defn delete-file [key db]
  (with-db [conn db]
    (sql-delete-file! {:key key} conn)))

(defn get-metadata [key-list db]
  (with-db [conn db]
    (->> (sql-get-metadata {:keys key-list} conn)
         (map db-utils/unwrap-data)
         (reduce (fn pick-latest-metadata [result {:keys [key uploaded] :as metadata}]
                   (cond-> result
                     (or (not (contains? result key))
                         (t/before? (get-in result [key :uploaded]) uploaded))
                     (assoc key metadata)))
                 {})
         (map second))))

(defn get-unscanned-file [conn]
  {:pre [(map? conn)
         (contains? conn :connection)]} ; force transaction
  (->> (sql-get-unscanned-file {} conn)
       (map db-utils/unwrap-data)
       (first)))

(defn get-old-draft-files [db]
  (with-db [conn db]
    (->> (sql-get-draft-files {} conn)
         (map db-utils/unwrap-data))))

(defn set-virus-scan-status! [file-key status db]
  (with-db [conn db]
    (sql-set-virus-scan-status! {:file_key          file-key
                                 :virus_scan_status (name status)}
                                conn)))

(defn finalize-files [keys db]
  (with-db [conn db]
    (sql-finalize-files! {:keys keys} conn)))
