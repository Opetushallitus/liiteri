(ns liiteri.audit-log
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component])
  (:import [fi.vm.sade.auditlog Audit ApplicationType CommonLogMessageFields AbstractLogMessage]))

(def ^:private date-time-formatter (f/formatter :date-time))

(def operation-new "lisäys")
(def operation-finalize "vahvistus")
(def operation-delete "poisto")
(def operation-query "haku")

(defprotocol AuditLog
  (log [this id operation message]))

(defrecord AuditLogger []
  component/Lifecycle

  (start [this]
    (assoc this :logger (Audit. "liiteri" ApplicationType/BACKEND)))

  (stop [this]
    (assoc this :logger nil))

  AuditLog

  (log [this id operation message]
    (let [logger    (:logger this)
          timestamp (f/unparse date-time-formatter (t/now))
          id-map    {:file-keys (if (string? id) [id] id)}
          log-map   {CommonLogMessageFields/ID        (json/generate-string id-map)
                     CommonLogMessageFields/TIMESTAMP timestamp
                     CommonLogMessageFields/OPERAATIO operation
                     CommonLogMessageFields/MESSAGE   (json/generate-string message)}]
      (->> (proxy [AbstractLogMessage] [log-map])
           (.log logger)))))

(defn new-logger []
  (map->AuditLogger {}))
