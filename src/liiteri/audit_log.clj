(ns liiteri.audit-log
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component])
  (:import [fi.vm.sade.auditlog Audit ApplicationType CommonLogMessageFields AbstractLogMessage]))

(def ^:private date-time-formatter (f/formatter :date-time))

(def operation-new "lisäys")
(def operation-delete "poisto")
(def operation-query "haku")

(defn- not-blank? [string]
  (not (clojure.string/blank? string)))

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
    {:pre [(or (and (string? id)
                    (not-blank? id))
               (and (vector? id)
                    (every? not-blank? id)))
           (some #{operation} [operation-new operation-delete operation-query])
           (some? message)]}
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
