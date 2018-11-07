(ns liiteri.server
  (:require [com.stuartsierra.component :as component]
            [liiteri.api :as api]
            [aleph.http :as http-server]
            [taoensso.timbre :as log]))

(defrecord Server [config]
  component/Lifecycle

  (start [this]
    (let [port   (get-in config [:server :port] 16832)
          api    (api/new-api this)
          server (http-server/start-server api {:port port})]
      (log/info (str "Started server on port " port))
      (assoc this :server server)))

  (stop [this]
    (when-let [server (:server this)]
      (.close server)
      (log/info (str "Stopped server")))
    (assoc this :server nil)))

(defn new-server []
  (map->Server {}))
