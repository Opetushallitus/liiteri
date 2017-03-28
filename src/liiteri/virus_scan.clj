(ns liiteri.virus-scan
  (:require [chime :as c]
            [clojure.core.async :as a]
            [clj-time.core :as t]
            [clj-time.periodic :as p]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defn- scan-files [db])

(defrecord Scanner [db]
  component/Lifecycle

  (start [this]
    (let [times (c/chime-ch (p/periodic-seq (t/now) (t/seconds 5))
                             {:ch (a/chan (a/sliding-buffer 1))})]
      (a/go-loop []
        (when-let [_ (a/<! times)]
          (scan-files db)
          (recur)))
      (assoc this :chan times)))

  (stop [this]
    (when-let [chan (:chan this)]
      (a/close! chan)
      (assoc this :chan nil))))

(defn new-scanner []
  (map->Scanner {}))
