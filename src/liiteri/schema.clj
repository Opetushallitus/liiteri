(ns liiteri.schema
  (:require [ring.swagger.upload]
            [schema.core :as s])
  (:import (java.time ZonedDateTime)))

;; This is the public schema of Liiteri API

(s/defschema Preview
  {:key          s/Str
   :content-type s/Str
   :size         s/Int
   :uploaded     ZonedDateTime
   :deleted      (s/maybe ZonedDateTime)})

(s/defschema File
  {:key               s/Str
   :filename          s/Str
   :content-type      s/Str
   :size              s/Int
   :page-count        (s/maybe s/Int)
   :virus-scan-status s/Str
   :final             s/Bool
   :uploaded          ZonedDateTime
   :deleted           (s/maybe ZonedDateTime)
   :preview-status    (s/enum "not_supported" "not_generated" "started" "finished" "error")
   :previews          [Preview]
   (s/optional-key :content-disposition) (s/maybe s/Str)})
