(ns liiteri.schema
  (:require [ring.swagger.upload]
            [schema.core :as s])
  (:import [org.joda.time DateTime]))

(s/defschema File
  {:id                       s/Str
   :filename                 s/Str
   :content-type             s/Str
   :uploaded                 DateTime
   (s/optional-key :deleted) (s/maybe DateTime)})
