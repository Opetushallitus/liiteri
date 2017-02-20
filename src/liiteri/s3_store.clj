(ns liiteri.s3-store
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [liiteri.db.file-store :as file-store]
            [liiteri.utils :as utils]
            [ring.swagger.upload])
  (:import [java.util UUID]
           [java.io File]))

(defn create-file [file s3-client db]
  {:pre [(instance? File (:tempfile file))
         (utils/not-blank? (:filename file))
         (utils/not-blank? (:content-type file))]}
  (let [id        (str (UUID/randomUUID))
        s3-object (.putObject (:s3-client s3-client) "oph-liiteri-dev" id (:tempfile file))]
    (jdbc/with-db-transaction [datasource db]
      (let [conn    {:connection datasource}
            file    (file-store/create-file (assoc (select-keys file [:filename :content-type]) :id id) conn)
            version (file-store/create-version (.getVersionId s3-object) id conn)]
        (assoc file :uploaded (:uploaded version))))))

(defn- not-deleted? [{:keys [deleted]}]
  (or (nil? deleted)
      (t/after? deleted (t/now))))

(defn update-file [file id s3-client db]
  {:pre [(instance? File (:tempfile file))
         (utils/not-blank? (:filename file))
         (utils/not-blank? (:content-type file))
         (utils/not-blank? id)]}
  (jdbc/with-db-transaction [datasource db]
    (let [conn              {:connection datasource}
          previous-versions (file-store/get-file-for-update id conn)]
      (when (every? not-deleted? previous-versions)
        (let [s3-object (.putObject (:s3-client s3-client) "oph-liiteri-dev" id (:tempfile file))
              version   (file-store/create-version (.getVersionId s3-object) id conn)]
          (merge (select-keys file [:filename :content-type])
                 {:id id :uploaded (:uploaded version)}))))))

(defn delete-file [id s3-client db]
  (let [client  (:s3-client s3-client)
        deleted (file-store/delete-file id db)]
    (when (> deleted 0)
      (.deleteObject client "oph-liiteri-dev" (str id)))
    deleted))
