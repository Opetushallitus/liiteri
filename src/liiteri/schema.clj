(ns liiteri.schema
  (:require [ring.swagger.upload]
            [schema.core :as s])
  (:import [ring.swagger.upload Upload]
           [org.joda.time DateTime]))

(def FileUpload
  (Upload. {:id           s/Int
            :filename     s/Str
            :content-type s/Str
            :uploaded     DateTime}))
