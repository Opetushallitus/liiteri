(ns liiteri.db.test-metadata-store
  (:require [liiteri.db.db-utils :as db-utils]
            [yesql.core :as sql]))

(sql/defqueries "sql/test-files.sql")

(defn get-metadata-for-tests [key-list db]
  (let [conn {:connection db}]
    (->> (sql-get-metadata-for-tests {:keys key-list} conn)
         (map db-utils/unwrap-data)
         (first))))

(defn create-file [spec db]
  (let [conn {:connection db}]
    (-> (db-utils/kwd->snake-case spec)
        (sql-create-file<! conn)
        (db-utils/unwrap-data))))
