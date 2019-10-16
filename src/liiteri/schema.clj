(ns liiteri.schema
  (:require [ring.swagger.upload]
            [schema.core :as s])
  (:import [org.joda.time DateTime]))

;; This is the public schema of Liiteri API

(s/defschema Preview
  {:key                       s/Str
   :content-type              s/Str
   :size                      s/Int
   (s/optional-key :uploaded) DateTime
   (s/optional-key :deleted)  (s/maybe DateTime)})

(s/defschema File
  {:key                       s/Str
   :filename                  s/Str
   :content-type              s/Str
   :size                      s/Int
   :page-count                (s/maybe s/Int)
   :virus-scan-status         s/Str
   :final                     s/Bool
   (s/optional-key :uploaded) DateTime
   (s/optional-key :deleted)  (s/maybe DateTime)
   :preview-status            (s/enum "not_supported" "not_generated" "finished")
   (s/optional-key :previews)  [Preview]})
