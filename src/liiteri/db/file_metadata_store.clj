(ns liiteri.db.file-metadata-store
  (:require [clj-time.core :as t]
            [clojure.string :as string]
            [liiteri.db.db-utils :as db-utils]
            [schema.core :as s]
            [yesql.core :as sql]
            [liiteri.schema :as schema]
            [taoensso.timbre :as log]
            [string-normalizer.filename-normalizer :as normalizer])
  (:import [java.text Normalizer Normalizer$Form]))

(declare sql-create-file<!)
(declare sql-delete-file!)
(declare sql-delete-preview!)
(declare sql-get-previews)
(declare sql-get-metadata)
(declare sql-get-unscanned-file)
(declare sql-get-draft-file)
(declare sql-get-draft-preview)
(declare sql-get-file-without-mime-type)
(declare sql-get-file-without-preview)
(declare sql-set-virus-scan-status!)
(declare sql-mark-virus-scan-for-retry-or-fail)
(declare sql-finalize-files!)
(declare sql-set-content-type-and-filename!)
(declare sql-get-queue-length)
(declare sql-get-oldest-unscanned-file)
(declare sql-create-preview<!)
(declare sql-set-file-page-count-and-preview-status!)
(declare sql-mark-previews-final!)
(declare sql-update-filename!)

(sql/defqueries "sql/files.sql")

(def ^:private non-valid-character-pattern #"(?i)[^a-z\.\-_0-9 ]")
(def ^:private empty-filename-pattern #"(?i)^\.([a-z0-9]+)$")

(defn- sanitize [string]
  (let [sanitized (-> string
                      (string/replace #"ä" "a")
                      (string/replace #"Ä" "A")
                      (string/replace #"ö" "o")
                      (string/replace #"Ö" "O")
                      (string/replace #"å" "a")
                      (string/replace #"Å" "a")
                      (string/replace non-valid-character-pattern ""))
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
      (assoc :previews [])
      (dissoc :virus-scan-retry-count)
      (dissoc :virus-scan-retry-after)
      (dissoc :id)))

(defn delete-file [key conn]
  (sql-delete-file! {:key key} conn))

(defn delete-preview [key conn]
  (sql-delete-preview! {:key key} conn))

(defn- fix-null-content-type [metadata]
  (if (= nil (:content-type metadata))
    (assoc metadata :content-type "application/octet-stream")
    metadata))

(defn- add-previews [file conn]
  (->> (sql-get-previews {:file_key (:key file)} conn)
       (map db-utils/unwrap-data)
       (assoc file :previews)))

(defn get-metadata [key-list conn]
  (->> (sql-get-metadata {:keys key-list} conn)
       (eduction (map db-utils/unwrap-data)
                 (map fix-null-content-type)
                 (map #(add-previews % conn)))
       (reduce (fn pick-latest-metadata [result {:keys [key uploaded] :as metadata}]
                 (cond-> result
                         (or (not (contains? result key))
                             (t/before? (get-in result [key :uploaded]) uploaded))
                         (assoc key metadata)))
               {})
       ((fn [metadata]
          (keep #(get metadata %) key-list)))))

(s/defn update-filename!
  [file-key :- s/Str
   normalized-filename :- s/Str
   conn :- s/Any]
  (sql-update-filename! {:file_key file-key
                         :filename normalized-filename}
                        conn))

(s/defn get-normalized-metadata! :- [schema/File]
  [key-list :- [s/Str]
   conn :- s/Any]
  (->> (get-metadata key-list conn)
       (mapv (fn [{file-key     :key
                   old-filename :filename
                   :as          metadata}]
               (let [normalized-filename (normalizer/normalize-filename old-filename)]
                 (when (and (not= normalized-filename old-filename)
                            (= (update-filename! file-key normalized-filename conn) 1))
                   (log/info
                     (format "Normalized filename, file key: %s, old filename: %s, normalized filename: %s"
                             file-key old-filename normalized-filename)))
                 (assoc metadata :filename normalized-filename))))))

(defn get-previews [file-key conn]
  (->> (sql-get-previews {:file_key file-key} conn)
       (eduction (map db-utils/unwrap-data))))

(defn get-unscanned-file [conn]
  (->> (sql-get-unscanned-file {} conn)
       (eduction (map db-utils/unwrap-data))
       (first)))

(defn get-old-draft-file [conn]
  (->> (sql-get-draft-file {} conn)
       (map db-utils/unwrap-data)
       (first)))

(defn get-old-draft-preview [conn]
  (->> (sql-get-draft-preview {} conn)
       (map db-utils/unwrap-data)
       (first)))

(defn get-file-without-mime-type [conn]
  (->> (sql-get-file-without-mime-type {} conn)
       (eduction (map db-utils/unwrap-data))
       (first)))

(defn get-file-without-preview [conn mime-types]
  (->> (sql-get-file-without-preview {:content_types mime-types} conn)
       (eduction (map db-utils/unwrap-data))
       (first)))

(defn set-virus-scan-status! [file-key status conn]
  (sql-set-virus-scan-status! {:file_key          file-key
                               :virus_scan_status (name status)}
                              conn))

(defn mark-virus-scan-for-retry-or-fail [file-key max-retry-count retry-wait-minutes conn]
  (->> (sql-mark-virus-scan-for-retry-or-fail {:file_key           file-key
                                               :retry_max_count    max-retry-count
                                               :retry_wait_minutes retry-wait-minutes}
                                              conn)
       first
       db-utils/unwrap-data))

(defn finalize-files [keys conn]
  (sql-finalize-files! {:keys keys} conn))

(defn set-content-type-and-filename! [file-key filename content-type conn]
  (sql-set-content-type-and-filename! {:file_key     file-key
                                       :filename     filename
                                       :content_type content-type}
                                      conn))

(defn get-queue-length
  [conn]
  (->> conn
       (sql-get-queue-length {})
       (first)
       :count))

(defn get-oldest-unscanned-file
  [conn]
  (->> conn
       (sql-get-oldest-unscanned-file {})
       (first)))

(defn save-preview! [file-key preview-key page-number preview-filename content-type size conn]
  (sql-create-preview<! {:file_key     file-key
                         :page_number  page-number
                         :filename     preview-filename
                         :key          preview-key
                         :content_type content-type
                         :size         size}
                        conn))

(defn set-file-page-count-and-preview-status! [key page-count preview-status conn]
  (sql-set-file-page-count-and-preview-status! {:key            key
                                                :page_count     page-count
                                                :preview_status preview-status}
                                               conn))

(defn mark-previews-final! [file-key conn]
  (sql-mark-previews-final! {:file_key file-key} conn))
