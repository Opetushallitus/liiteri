(ns liiteri.virus-scan
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [liiteri.db.file-metadata-store :as metadata-store]
            [org.httpkit.client :as http]
            [taoensso.timbre :as log]))

(defn- scan-file [db storage-engine config]
  (jdbc/with-db-transaction [datasource db]
    (let [conn {:connection db}]
      (when-let [{file-key :key filename :filename} (metadata-store/get-unscanned-file conn)]
        (let [file   (.get-file storage-engine file-key)
              url    (str (get-in config [:av :clamav-url]) "/scan")
              params {:form-params {"name" filename}
                      :multipart   [{:name "file" :content file :filename filename}]}
              resp   @(http/post url params)]
          (when (= (:status resp) 200)
            (let [status (if (= "Everything ok : true\n" (:body resp))
                           :done
                           :failed)]
              (metadata-store/set-virus-scan-status! file-key status conn))))))))

(defn- scan-files [db storage-engine config]
  (loop []
    (when (scan-file db storage-engine config)
      (recur))))

(defrecord Scanner [db storage-engine config]
  component/Lifecycle

  (start [this]
    (let [times (c/chime-ch (p/periodic-seq (t/now) (t/seconds 1))
                            {:ch (a/chan (a/sliding-buffer 1))})]
      (a/go-loop []
        (when-let [_ (a/<! times)]
          (scan-files db storage-engine config)
          (recur)))
      (assoc this :chan times)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan))
    (assoc this :chan nil)))

(defn new-scanner []
  (map->Scanner {}))
