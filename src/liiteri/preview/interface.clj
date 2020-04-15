(ns liiteri.preview.interface)

;; Multimethod that dispatches the file to correct generation function by content-type
(defmulti generate-previews-for-file (fn [_ file _ _] (:content-type file)))

;; Mark unhandled content-type as unsupported by default
(defmethod generate-previews-for-file :default [_ _ _ _]
  [nil []])
