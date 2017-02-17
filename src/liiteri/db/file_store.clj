(ns liiteri.db.file-store
  (:require [liiteri.db.db-utils :as db-utils]
            [yesql.core :as sql]))

(sql/defqueries "sql/files.sql")

(defn- not-blank? [string]
  (not (clojure.string/blank? string)))

(defn create-file [spec db]
  {:pre [(not-blank? (:filename spec))
         (not-blank? (:content-type spec))]}
  (let [conn {:connection db}]
    (-> (db-utils/kwd->snake-case spec)
        (sql-create-file<! conn)
        (db-utils/unwrap-data))))
