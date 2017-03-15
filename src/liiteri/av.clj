(ns liiteri.av
  (:require [taoensso.timbre :as log]
            [org.httpkit.client :as http]
            [liiteri.s3-store :as s3-store]
            [liiteri.db.file-store :as file-store]))

(defn check-db-file [file db s3-client]
  (let [file-stream (s3-store/get-file-stream (:key file) s3-client)
        url (str (System/getProperty "clamav.url" "http://localhost:8880/scan"))
        options {:form-params {"name" (:filename file)}
                 :multipart [{:name "file" :content file-stream :filename (:filename file)}]}
        {:keys [status headers body error] :as resp} @(http/post url options)]
    (if error
      error
      (if (.contains body "true")
        (do (file-store/mark-virus-checked (:key file) db)
            true)
        (do (log/info (str "file " (:key file) " contains a virus, deleting it"))
            (s3-store/delete-file (:key file) db s3-client)
            false)))))

(defn check-db-files [db s3-client]
  (let [files (file-store/get-unchecked-files db)
        response []]
    ;(map )
    (doseq [i files] (check-db-file i db s3-client))))

(defn check-multipart-file [file]
  (let [url (str (System/getProperty "clamav.url" "http://localhost:8880/scan"))]
    (let [options {:form-params {"name" (:filename file)}
                   :multipart [{:name "file" :content (:tempfile file) :filename (:filename file)}]}

          {:keys [status headers body error] :as resp} @(http/post url options)]
      (if error
        error
        true))))
