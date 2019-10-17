(ns liiteri.preview.preview-generator
  (:require [liiteri.db.file-metadata-store :as metadata-store]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [liiteri.files.file-store :as file-store]
            [clojure.java.io :as io])
  (:import (java.util.concurrent Executors TimeUnit ScheduledFuture)
           (java.io InputStream)
           (org.apache.pdfbox.pdmodel PDDocument)))

(def mime-types ["application/pdf"])

(defn- drain-stream [stream]
  (let [buffer (byte-array 65536)]
    (while (> (.read stream buffer) 0))))

(defn generate-file-previews [conn
                              storage-engine
                              {file-key :key
                               filename :filename
                               uploaded :uploaded}]
  (let [start-time (System/currentTimeMillis)]
    (try
      (let []
        (log/info (str "Generating previews for '" filename "' with key '" file-key "', uploaded on " uploaded " ..."))
        true)
      (catch Exception e
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
