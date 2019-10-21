(ns liiteri.preview.interface
  (:require [liiteri.db.file-metadata-store :as metadata-store]))

;; Multimethod that dispatches the file to correct generation function by content-type
(defmulti generate-previews-for-file (fn [conn storage-engine file input-stream max-page-count] (:content-type file)))

;; Mark unhandled content-type as unsupported by default
(defmethod generate-previews-for-file :default [conn storage-engine {file-key :file-key} _ _]
  [nil []])
