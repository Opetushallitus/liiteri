(ns liiteri.s3-store
  (:require [clojure.java.jdbc :as jdbc]
            [liiteri.db.file-store :as file-store]
            [ring.swagger.upload]))

(defn s3-store
  [s3-client db]
  (fn [item]
    (jdbc/with-db-transaction [datasource db]
      (let [conn     {:connection datasource}
            file     (file-store/create-file (select-keys item [:filename :content-type]) conn)
            client   (:s3-client s3-client)
            result   (.putObject client "oph-liiteri-dev" (str (:id file)) (:stream item) nil)
            version  (file-store/create-version (.getVersionId result) (:id file) conn)
            uploaded (:uploaded version)]
        (assoc file :uploaded uploaded)))))

(defn delete-file [id s3-client db]
  (let [client  (:s3-client s3-client)
        deleted (file-store/delete-file id db)]
    (when (> deleted 0)
      (.deleteObject client "oph-liiteri-dev" (str id)))
    deleted))
