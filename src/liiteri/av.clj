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

(defn check-db-files [db s3-client]
  (let [files (file-store/get-unchecked-files db)
        response []]
    ;(map )
    (doseq [i files] (check-db-file i db s3-client))))

(defrecord Av []
  component/Lifecycle

  (start [this]
    (let [url (:clamav-url (:av (:config this)))
          channel (a/chan)
          chimes (chime/chime-ch (periodic-seq (t/now) (-> 5 t/seconds)) channel)]
      (a/<!! (go-loop []
               (when-let [msg (<! chimes)]
                 (check-db-files (:db this) (:s3-client this))
                 (recur))))
      (assoc this :av nil)
      (assoc this :clam-url url)
      (assoc this :channel channel)))

  (stop [this]
    (when-let [channel (:channel this)]
      (a/close! channel)
      (log/info (str "Stopped server")))
    (assoc this :server nil)))


(defn new-av []
  (->Av))

