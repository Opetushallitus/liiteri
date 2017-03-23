(ns liiteri.server
  (:require [com.stuartsierra.component :as component]
            [liiteri.api :as api]
            [org.httpkit.server :as server]
            [taoensso.timbre :as log]))

(defrecord Server []
  component/Lifecycle

  (start [this]
    (let [port    (Long/valueOf (System/getProperty "server.port" "16832"))
          api     (api/new-api this)
          server  (server/run-server api {:port port})]
      (log/info (str "Started server on port " port))
      (assoc this :server server)))

  (stop [this]
    (when-let [stop (:server this)]
      (stop)
      (log/info (str "Stopped server")))
    (assoc this :server nil)))

(defn new-server []
  (->Server))
