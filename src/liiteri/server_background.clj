; backgroud-palvelulle käynnistetään serveri vain jotta healthcheck-kutsut ovat mahdollisia
(ns liiteri.server_background
  (:require [com.stuartsierra.component :as component]
            [aleph.http :as http-server]
            [taoensso.timbre :as log]
            [clj-access-logging]
            [clj-timbre-access-logging]
            [clojure.java.io :as io]
            [compojure.api.sweet :as api]
            [environ.core :refer [env]]
            [ring.util.http-response :as response]))

(defn new-api [config]
  (-> (api/api {}
               (api/context "/liiteri-background" []
                            (api/undocumented
                              (api/GET "/buildversion.txt" []
                                       (response/ok (slurp (io/resource "buildversion.txt")))))))
      (clj-access-logging/wrap-access-logging)
      (clj-timbre-access-logging/wrap-timbre-access-logging
        {:path (str (get-in config [:access-log :path])
                    (when (:hostname env) (str "_" (:hostname env))))})))

(defrecord Server [config]
  component/Lifecycle

  (start [this]
    (let [port   (get-in config [:server :port] 16832)
          api    (new-api config)
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
