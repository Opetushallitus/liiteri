(ns liiteri.s3-store
  (:require [clojure.java.jdbc :as jdbc]
            [liiteri.db.file-store :as file-store]
            [ring.swagger.upload])
  (:import [java.util UUID]))

(defn s3-store
  [s3-client db]
  (fn [item]
    (let [id        (str (UUID/randomUUID))
          s3-object (.putObject (:s3-client s3-client) "oph-liiteri-dev" id (:stream item) nil)]
      (jdbc/with-db-transaction [datasource db]
        (let [conn    {:connection datasource}
              file    (file-store/create-file (assoc (select-keys item [:filename :content-type]) :id id) conn)
              version (file-store/create-version (.getVersionId s3-object) id conn)]
          (assoc file :uploaded (:uploaded version)))))))

(defn delete-file [id s3-client db]
  (let [client  (:s3-client s3-client)
        deleted (file-store/delete-file id db)]
    (when (> deleted 0)
      (.deleteObject client "oph-liiteri-dev" (str id)))
    deleted))
