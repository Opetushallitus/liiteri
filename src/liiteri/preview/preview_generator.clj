(ns liiteri.preview.preview-generator
  (:require [liiteri.db.file-metadata-store :as metadata-store]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [liiteri.files.file-store :as file-store]
            [liiteri.preview.pdf-to-png :as pdf-to-png])
  (:import (java.util.concurrent Executors TimeUnit ScheduledFuture)
           (java.io InputStream)
           (org.apache.tika.io TikaInputStream)
           (java.util UUID)))

(def mime-types ["application/pdf"])

(defn generate-file-previews [conn
                              storage-engine
                              {file-key :key
                               filename :filename
                               uploaded :uploaded}]
  (let [start-time (System/currentTimeMillis)]
    (try
      (log/info (str "Generating previews for '" filename "' with key '" file-key "', uploaded on " uploaded " ..."))
      (with-open [^InputStream file                  (file-store/get-file storage-engine file-key)
                  ^TikaInputStream tika-input-stream (TikaInputStream/get file)]
        (let [pngs       (pdf-to-png/inputstream->pngs tika-input-stream)
              page-count (count pngs)]
          (doseq [[index png] (map-indexed vector pngs)]
            (let [ ;png-key (str file-key "-" index) TODO: previews.key field is not long enough for this
                  png-key (str (UUID/randomUUID))
                  page-filename (str "page_" index "_of_" filename)]
              (file-store/create-file-from-bytearray storage-engine png png-key)
              (metadata-store/save-preview! file-key png-key index page-filename png conn)))
          (metadata-store/set-file-page-count-and-preview-status! file-key page-count "finished" conn)
          (log/info (str "Generated " page-count " preview pages for '" filename "' with key '" file-key
                         "', uploaded on " uploaded " , took " (- (System/currentTimeMillis) start-time) " ms"))
          true))
      (catch Exception e
        (log/error e (str " Failed to generate previews for '" filename "' with key '" file-key "', uploaded on " uploaded " . "))
        ; TODO : Mark unsuccessful generation to db, to avoid congestion
        false))))

(defn- generate-next-preview [db storage-engine]
  (try
    (jdbc/with-db-transaction [tx db]
                              (let [conn {:connection tx}]
                                (if-let [file (metadata-store/get-file-without-preview conn mime-types)]
                                  (generate-file-previews conn storage-engine file)
                                  (do
                                    (log/info "Preview generation seems to be finished (or errored).")
                                    false))))
    (catch Exception e
      (log/error e "Failed to generate preview for the next file")
      false)))

(defn- generate-previews [db storage-engine]
  (try (loop []
         (when (generate-next-preview db storage-engine)
           (recur)))
       (catch Throwable t
         (println "Unexpected throwable!")
         (.printStackTrace t))))

(defprotocol Generator
  (generate-previews! [this]))

(defrecord PreviewGenerator [db storage-engine config]
  component/Lifecycle

  (start [this]
    (log/info "Starting document preview generation process...")
    (let [poll-interval (get-in config [:preview-generator :poll-interval-seconds])
          scheduler (Executors/newScheduledThreadPool 1)
          preview-generator #(generate-previews db storage-engine)
          time-unit TimeUnit/SECONDS
          preview-generator-future (.scheduleAtFixedRate scheduler preview-generator 0 poll-interval time-unit)]
      (log/info (str "Started document preview generation process, restarting at " poll-interval " " time-unit " intervals."))
      (assoc this :preview-generator-future preview-generator-future)))

  (stop [this]
    (when-let [^ScheduledFuture preview-generator-future (:preview-generator-future this)]
      (.cancel preview-generator-future true))
    (log/info "Stopped MIME type fixing process")
    (assoc this :fixer-future nil))

  Generator

  (generate-previews! [this]
    (generate-previews db storage-engine)))

(defn new-preview-generator []
  (map->PreviewGenerator {}))
