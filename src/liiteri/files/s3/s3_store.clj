(ns liiteri.files.s3.s3-store
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [liiteri.db.file-metadata-store :as metadata-store]
            [ring.swagger.upload])
  (:import [java.util UUID]
           [java.io File]))

(defn create-file [file s3-client db]
  (let [key       (str (UUID/randomUUID))
        s3-object (.putObject (:s3-client s3-client) "oph-liiteri-dev" key (:tempfile file))]
    (jdbc/with-db-transaction [datasource db]
      (let [conn {:connection datasource}]
        (metadata-store/create-file (merge (select-keys file [:filename :content-type :size])
                                           {:key key :version (.getVersionId s3-object)})
                                    conn)))))

(defn- not-deleted? [{:keys [deleted]}]
  (or (nil? deleted)
      (t/after? deleted (t/now))))

(defn update-file [file key s3-client db]
  (jdbc/with-db-transaction [datasource db]
    (let [conn              {:connection datasource}
          previous-versions (metadata-store/get-file-for-update key conn)]
      (when (every? not-deleted? previous-versions)
        (let [s3-object (.putObject (:s3-client s3-client) "oph-liiteri-dev" key (:tempfile file))]
          (metadata-store/create-file (merge (select-keys file [:filename :content-type :size])
                                             {:key key :version (.getVersionId s3-object)})
                                      conn))))))

(defn delete-file [key s3-client db]
  (let [client  (:s3-client s3-client)
        deleted (metadata-store/delete-file key db)]
    (when (> deleted 0)
      (.deleteObject client "oph-liiteri-dev" key))
    deleted))

(defn get-file [key s3-client db]
  (let [metadata (metadata-store/get-metadata key db)
        client   (:s3-client s3-client)]
    (when (> (count metadata) 0)
      (-> (.getObject client "oph-liiteri-dev" key)
          (.getObjectContent)))))
