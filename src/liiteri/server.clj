(ns liiteri.server
  (:require [com.stuartsierra.component :as component]
            [liiteri.api :as api]
            [org.httpkit.server :as server]
            [taoensso.timbre :as log]))

(defrecord Server [config]
  component/Lifecycle

  (start [this]
    (let [port   (get-in config [:server :port] 16832)
          api    (api/new-api this)
          max-attachment-size (read-string
                                (or
                                  (not-empty (get-in config [:file-store :attachment-max-size-bytes]))
                                  "1073741824"))
          server (server/run-server api {:port     port
                                         :max-body max-attachment-size})]
      (log/info (str "Started server on port " port))
      (assoc this :server server)))

  (stop [this]
    (when-let [stop (:server this)]
      (stop)
      (log/info (str "Stopped server")))
    (assoc this :server nil)))

(defn new-server []
  (map->Server {}))
