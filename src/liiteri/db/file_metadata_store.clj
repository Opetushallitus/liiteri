(ns liiteri.db.file-metadata-store
  (:require [clj-time.core :as t]
            [liiteri.db.db-utils :as db-utils]
            [liiteri.schema :as schema]
            [schema-tools.core :as st]
            [yesql.core :as sql])
  (:import [org.joda.time DateTime]
           (java.text Normalizer Normalizer$Form)))

(sql/defqueries "sql/files.sql")

(def ^:private non-valid-character-pattern #"(?i)[^a-z\.\-_0-9 ]")
(def ^:private empty-filename-pattern #"(?i)^\.([a-z0-9]+)$")

(defn- sanitize [string]
  (let [sanitized (-> string
                      (clojure.string/replace #"ä" "a")
                      (clojure.string/replace #"Ä" "A")
                      (clojure.string/replace #"ö" "o")
                      (clojure.string/replace #"Ö" "O")
                      (clojure.string/replace #"å" "a")
                      (clojure.string/replace #"Å" "a")
                      (clojure.string/replace non-valid-character-pattern ""))
        matcher   (re-matcher empty-filename-pattern sanitized)
        extension (second (re-find matcher))]
    (if (empty? extension)
      sanitized
      (str "liite." extension))))

(defn- unicode-normalize
  "Normalize given string to Unicode Normalization Form C (NFC), which uses
  canonical decomposition and then canonical composition. See
  <http://www.unicode.org/reports/tr15/tr15-23.html> for more information
  on Unicode Normalization Forms."
  [s]
  (Normalizer/normalize s Normalizer$Form/NFC))

(defn create-file [spec conn]
  (-> (db-utils/kwd->snake-case spec)
      (update :filename unicode-normalize)
      (sql-create-file<! conn)
      (db-utils/unwrap-data)
      (update :filename sanitize)
      (dissoc :id)))

(defn delete-file [key conn]
  (sql-delete-file! {:key key} conn))

(defn get-metadata [key-list conn]
  (->> (sql-get-metadata {:keys key-list} conn)
       (eduction (map db-utils/unwrap-data)
                 (map #(update % :filename sanitize)))
       (reduce (fn pick-latest-metadata [result {:keys [key uploaded] :as metadata}]
                 (cond-> result
                   (or (not (contains? result key))
                       (t/before? (get-in result [key :uploaded]) uploaded))
                   (assoc key metadata)))
               {})
       (map second)))

(defn get-unscanned-file [conn]
  (->> (sql-get-unscanned-file {} conn)
       (eduction (map db-utils/unwrap-data)
                 (map #(update % :filename sanitize)))
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
