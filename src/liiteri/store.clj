(ns liiteri.store
  (:require [ring.swagger.upload]
            [schema.core :as s])
  (:import [ring.swagger.upload Upload]
           [java.io InputStream]))

(def StreamUpload
  (Upload. {:filename     s/Str
            :content-type s/Str}))

(defn stream-store
  []
  (fn [item]
    (select-keys item [:filename :content-type])))
