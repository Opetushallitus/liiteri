(ns liiteri.preview.preview-generator
  (:require [liiteri.db.file-metadata-store :as metadata-store]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [liiteri.files.file-store :as file-store]
            [liiteri.preview.interface :as interface]
            [liiteri.preview.pdf :as pdf])
  (:import (java.util.concurrent Executors TimeUnit ScheduledFuture)))

(def content-types-to-process (concat pdf/content-types))

(def were-unprocessed-files-found-on-last-run (atom true))

(defn save-bytearray-as-preview [conn storage-engine file-key page-number preview-key preview-filename content-type data-as-byte-array]
  (file-store/create-file-from-bytearray storage-engine
                                         data-as-byte-array
                                         preview-key)
  (metadata-store/save-preview! file-key
                                preview-key
                                page-number
                                preview-filename
                                content-type
                                (count data-as-byte-array)
                                conn))

(defn generate-file-previews [config conn storage-engine file timeout-scheduler]
  (let [start-time (System/currentTimeMillis)
        {file-key :key
         filename :filename
         uploaded :uploaded} file]
    (try
      (log/info (format "Generating previews for '%s' with key '%s', uploaded on %s ..." filename file-key uploaded))
      (with-open [input-stream (file-store/get-file storage-engine file-key)]
        (let [preview-timeout-ms    (get-in config [:preview-generator :preview-timeout-ms] 45000)
              [page-count previews] (.invokeAny timeout-scheduler [#(interface/generate-previews-for-file storage-engine
                                                                          file
                                                                          input-stream
                                                                          config)] preview-timeout-ms TimeUnit/MILLISECONDS)]
          (doseq [[page-index preview-as-byte-array] (map-indexed vector previews)]
            (let [preview-key      (str file-key "." page-index)
                  preview-filename preview-key]
              (save-bytearray-as-preview conn
                                         storage-engine
                                         file-key
                                         page-index
                                         preview-key
                                         preview-filename
                                         "image/png"
                                         preview-as-byte-array)))

          (if (not (nil? page-count))
            (do
              (metadata-store/set-file-page-count-and-preview-status! file-key page-count "finished" conn)
              (metadata-store/mark-previews-final! file-key conn)
              (log/info (format "Generated %d preview pages for '%s' with key '%s', uploaded on %s, took %d ms"
                                (count previews)
                                filename
                                file-key
                                uploaded
                                (- (System/currentTimeMillis) start-time))))
            (do
              (log/info (format "Generated no preview pages for '%s' with key '%s'" filename file-key))
              (metadata-store/set-file-page-count-and-preview-status! file-key nil "not_supported" conn)))
          true))
      (catch Throwable e
        (log/error e (str " Failed to generate previews for '" filename "' with key '" file-key "', uploaded on " uploaded ": " e))
        (metadata-store/set-file-page-count-and-preview-status! file-key nil "error" conn)
        false))))

(defn- generate-next-preview [config db storage-engine timeout-scheduler]
  (try
    (jdbc/with-db-transaction [tx db]
      (let [conn {:connection tx}]
        (if-let [file (metadata-store/get-file-without-preview conn content-types-to-process)]
          (do
            (reset! were-unprocessed-files-found-on-last-run true)
            (generate-file-previews config conn storage-engine file timeout-scheduler))
          (do
            (when @were-unprocessed-files-found-on-last-run
              (log/info "Preview generation seems to be finished (or errored)."))
            (reset! were-unprocessed-files-found-on-last-run false)
            false))))
    (catch Exception e
      (log/error e "Failed to generate preview for the next file")
      false)))

(defn- generate-previews [config db storage-engine timeout-scheduler]
  (try (loop []
         (when (generate-next-preview config db storage-engine timeout-scheduler)
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
    (let [poll-interval            (get-in config [:preview-generator :poll-interval-seconds])
          scheduler                (Executors/newScheduledThreadPool 1)
          timeout-scheduler        (Executors/newCachedThreadPool)
          preview-generator        #(generate-previews config db storage-engine timeout-scheduler)
          time-unit                TimeUnit/SECONDS
          preview-generator-future (.scheduleAtFixedRate scheduler preview-generator 0 poll-interval time-unit)]
      (log/info (str "Started document preview generation process, restarting at " poll-interval " " time-unit " intervals."))
      (assoc this :preview-generator-future preview-generator-future)))

  (stop [this]
    (when-let [^ScheduledFuture preview-generator-future (:preview-generator-future this)]
      (.cancel preview-generator-future true))
    (log/info "Stopped preview generation process")
    (assoc this :preview-generator-future nil))

  Generator

  (generate-previews! [this]
    (generate-previews db storage-engine)))

(defn new-preview-generator []
  (map->PreviewGenerator {}))
