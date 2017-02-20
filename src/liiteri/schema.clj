(ns liiteri.schema
  (:require [ring.swagger.upload]
            [schema.core :as s])
  (:import [org.joda.time DateTime]))

;; This is the public schema of Liiteri API

(s/defschema File
  {:id                        s/Str
   :filename                  s/Str
   :content-type              s/Str
   (s/optional-key :uploaded) DateTime
   (s/optional-key :deleted)  (s/maybe DateTime)})

;; "Private" schema, used internally with schema.core/defn

(s/defschema Version {:id                       s/Int
                      :file-id                  s/Str
                      :version                  s/Str
                      :uploaded                 DateTime
                      (s/optional-key :deleted) (s/maybe DateTime)})

(s/defschema FileUpload {:filename     s/Str
                         :content-type s/Str
                         :size         s/Int
                         :tempfile     java.io.File})
