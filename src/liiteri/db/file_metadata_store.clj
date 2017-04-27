(ns liiteri.db.file-metadata-store
  (:require [clj-time.core :as t]
            [liiteri.db.db-utils :as db-utils]
            [liiteri.schema :as schema]
            [schema-tools.core :as st]
            [yesql.core :as sql])
  (:import [org.joda.time DateTime]))

(sql/defqueries "sql/files.sql")

(def ^:private non-valid-character-pattern #"(?i)[^a-z\.\-_0-9 ]")
(def ^:private empty-filename-pattern #"(?i)^\.([a-z0-9]+)$")

(defn- normalize [string]
  (let [normalized (-> string
                       (clojure.string/replace #"ä" "a")
                       (clojure.string/replace #"Ä" "A")
                       (clojure.string/replace #"ö" "o")
                       (clojure.string/replace #"Ö" "O")
                       (clojure.string/replace #"å" "a")
                       (clojure.string/replace #"Å" "a")
                       (clojure.string/replace non-valid-character-pattern ""))
        matcher    (re-matcher empty-filename-pattern normalized)
        extension  (second (re-find matcher))]
    (if (empty? extension)
      normalized
      (str "liite." extension))))

(defn create-file [spec conn]
  (-> (db-utils/kwd->snake-case spec)
      (sql-create-file<! conn)
      (db-utils/unwrap-data)
      (update :filename normalize)
      (dissoc :id)))

(defn delete-file [key conn]
  (sql-delete-file! {:key key} conn))

(defn get-metadata [key-list conn]
  (->> (sql-get-metadata {:keys key-list} conn)
       (eduction (map db-utils/unwrap-data)
                 (map #(update % :filename normalize)))
       (reduce (fn pick-latest-metadata [result {:keys [key uploaded] :as metadata}]
                 (cond-> result
                         (or (not (contains? result key))
                             (t/before? (get-in result [key :uploaded]) uploaded))
                         (assoc key metadata)))
               {})
       (map second)))

(defn get-unscanned-file [conn]
  {:pre [(map? conn)
         (contains? conn :connection)]} ; force transaction
  (->> (sql-get-unscanned-file {} conn)
       (eduction (map db-utils/unwrap-data)
                 (map #(update % :filename normalize)))
       (first)))

(defn get-old-draft-file [conn]
  (->> (sql-get-draft-file {} conn)
       (map db-utils/unwrap-data)
       (first)))

(defn set-virus-scan-status! [file-key status conn]
  (sql-set-virus-scan-status! {:file_key          file-key
                               :virus_scan_status (name status)}
                              conn))

(defn finalize-files [keys conn]
  (sql-finalize-files! {:keys keys} conn))
