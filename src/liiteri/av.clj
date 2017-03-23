(ns liiteri.av
  (:require [com.stuartsierra.component :as component]
            [chime :as chime]
            [clj-time.core :as t]
            [clj-time.periodic :refer [periodic-seq]]
            [clojure.core.async :as a :refer [<! go-loop]]
            [taoensso.timbre :as log]
            [org.httpkit.client :as http]
            [liiteri.s3-store :as s3-store]
            [liiteri.db.file-store :as file-store]))

(defn check-db-file [clamav-url file db s3-client]
  (let [file-stream (s3-store/get-file-stream (:key file) s3-client)
        options {:form-params {"name" (:filename file)}
                 :multipart [{:name "file" :content file-stream :filename (:filename file)}]}
        {:keys [status headers body error] :as resp} @(http/post clamav-url options)]
    (if error
      error
      (if (.contains body "true")
        (do (file-store/mark-virus-checked (:key file) db)
            true)
        (do (log/info (str "file " (:key file) " contains a virus, deleting it"))
            (s3-store/delete-file (:key file) "VIRUS" db s3-client)
            false)))))

(defn check-multipart-file [file]
  (let [url (str (System/getProperty "clamav.url" "http://localhost:8880/scan"))]
    (let [options {:form-params {"name" (:filename file)}
                   :multipart [{:name "file" :content (:tempfile file) :filename (:filename file)}]}

          {:keys [status headers body error] :as resp} @(http/post url options)]
      (if error
        error
        true))))

(defn check-db-files [clamav-url db s3-client]
  (let [files (file-store/get-unchecked-files db)
        response []]
    ;(map )
    (doseq [i files] (check-db-file clamav-url i db s3-client))))

(defn- scheduler [clamav-url db s3-client]
  (let [chimes (chime/chime-ch (periodic-seq (t/now) (-> 4 t/seconds)) (a/chan (a/dropping-buffer 1)))]
    (a/go (a/<!! (go-loop []
             (when-let [time (<! chimes)]
               (check-db-files clamav-url db s3-client)
               (recur)))))))

(defrecord Av []
  component/Lifecycle

  (start [this]
      (assoc this :clamav-url (:clamav-url (:av (:config this)))
                  :scheduler (scheduler (:clamav-url this) (:db this) (:s3-client this))))

  (stop [this]
    (let [scheduler (:scheduler this)]
      (scheduler)
      (dissoc this :clam-url :scheduler))))

(defn new-av []
  (->Av))