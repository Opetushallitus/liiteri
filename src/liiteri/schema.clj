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
   :virus-scan-status         s/Str
   :final                     s/Bool
   (s/optional-key :uploaded) DateTime
   (s/optional-key :deleted)  (s/maybe DateTime)})
