(ns liiteri.store
  (:require [liiteri.db.file-store :as file-store]
            [ring.swagger.upload]))

(defn stream-store
  [db]
  (fn [item]
    (file-store/create-file (select-keys item [:filename :content-type]) db)))
