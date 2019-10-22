(ns liiteri.preview.interface
  (:require [liiteri.db.file-metadata-store :as metadata-store]))

;; Multimethod that dispatches the file to correct generation function by content-type
(defmulti generate-previews-for-file (fn [storage-engine file input-stream config] (:content-type file)))

;; Mark unhandled content-type as unsupported by default
(defmethod generate-previews-for-file :default [_ _ _ _]
  [nil []])
