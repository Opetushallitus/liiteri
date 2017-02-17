(ns liiteri.s3-store
  (:require [liiteri.db.file-store :as file-store]
            [ring.swagger.upload]))

(defn s3-store
  [s3-client db]
  (fn [item]
    (let [file   (file-store/create-file (select-keys item [:filename :content-type]) db)
          client (:s3-client s3-client)
          key    (str (:id file))
          stream (:stream item)]
      (.putObject client "oph-liiteri-dev" key stream nil)
      file)))

(defn delete-file [id s3-client db]
  (let [client (:s3-client s3-client)
        key    (str id)]
    (when-let [file (file-store/delete-file id db)]
      (.deleteObject client "oph-liiteri-dev" key)
      file)))
