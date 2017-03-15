(ns liiteri.schema
  (:require [ring.swagger.upload]
            [schema.core :as s])
  (:import [org.joda.time DateTime]))

;; This is the public schema of Liiteri API

(s/defschema File
  {:key                       s/Str
   :filename                  s/Str
   :content-type              s/Str
   :size                      s/Int
   :version                   s/Str
   (s/optional-key :virus-checked)             (s/maybe DateTime)
   (s/optional-key :uploaded) DateTime
   (s/optional-key :deleted)  (s/maybe DateTime)})
