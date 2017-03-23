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
  (try
    (let [file-stream (s3-store/get-file-stream (:key file) s3-client)]
      (let [options {:form-params {"name" (:filename file)}
                     :multipart [{:name "file" :content file-stream :filename (:filename file)}]}
          {:keys [status headers body error] :as resp} @(http/post clamav-url options)]
      (if error
        (do (log/error error) error)
        (if (.contains body "true")
          (do (file-store/mark-virus-checked (:key file) db)
              true)
          (do (log/warn (str "file " (:key file) " contains a virus, deleting it"))
              (file-store/mark-virus-checked (:key file) db)
              (s3-store/delete-file (:key file) "VIRUS" s3-client db)
              false)))))
    (catch Exception e (log/error "error while checking viruses " (str e)))))

(defn check-multipart-file [file url]
  (let [options {:form-params {"name" (:filename file)}
                 :multipart [{:name "file" :content (:tempfile file) :filename (:filename file)}]}

        {:keys [status headers body error] :as resp} @(http/post url options)]
    (if error
      (do (log/error error) error)
      (do (log/info body) body))))

(defn check-db-files [clamav-url db s3-client]
  (let [files (file-store/get-unchecked-files db)
        response []]
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
    (let [clamav-url (:clamav-url (:av (:config this)))]
      (assoc this :clamav-url clamav-url
                  :scheduler (scheduler clamav-url (:db this) (:s3-client this)))))

  (stop [this]
    (when-let [scheduler (:scheduler this)]
      (a/close! scheduler)
      (dissoc this :clamav-url :scheduler))))

(defn new-av []
  (->Av))