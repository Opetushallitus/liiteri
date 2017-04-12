(ns liiteri.audit-log
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [cheshire.core :as json])
  (:import [fi.vm.sade.auditlog Audit ApplicationType CommonLogMessageFields AbstractLogMessage]))

(def ^:private logger (Audit. "liiteri" ApplicationType/BACKEND))
(def ^:private date-time-formatter (f/formatter :date-time))

(def operation-new "lisäys")
(def operation-delete "poisto")
(def operation-query "haku")

(defn- not-blank? [string]
  (not (clojure.string/blank? string)))

(defn log [{:keys [id operation message]}]
  {:pre [(or (and (string? id)
                  (not-blank? id))
             (and (vector? id)
                  (every? not-blank? id)))
         (some #{operation} [operation-new operation-delete operation-query])
         (some? message)]}
  (let [timestamp (f/unparse date-time-formatter (t/now))
        id-map    {:file-keys (if (string? id) [id] id)}
        log-map   {CommonLogMessageFields/ID        (json/generate-string id-map)
                   CommonLogMessageFields/TIMESTAMP timestamp
                   CommonLogMessageFields/OPERAATIO operation
                   CommonLogMessageFields/MESSAGE   (json/generate-string message)}]
    (clojure.pprint/pprint log-map)
    (->> (proxy [AbstractLogMessage] [log-map])
         (.log logger))))
