(ns liiteri.audit-log
  (:require [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]])
  (:import [fi.vm.sade.auditlog Audit ApplicationType Changes$Builder Logger Operation Target$Builder User]
           [org.ietf.jgss Oid]
           (java.net InetAddress)))

(deftype New []
  Operation
  (name [_] "lisäys"))

(deftype Finalize []
  Operation
  (name [_] "vahvistus"))

(deftype Delete []
  Operation
  (name [_] "poisto"))

(deftype FileQuery []
  Operation
  (name [_] "tiedosto haku"))

(deftype MetadataQuery []
  Operation
  (name [_] "metatieto haku"))

(def operation-new (->New))
(def operation-finalize (->Finalize))
(def operation-delete (->Delete))
(def operation-file-query (->FileQuery))
(def operation-metadata-query (->MetadataQuery))

(defn file-target [key]
  (timbre/info "AUDIT KEY:" key)
  (-> (new Target$Builder)
      (.setField "file-key" key)
      .build))

(defn new-file-changes [file]
  (-> (new Changes$Builder)
      (.added "size" (str (:size file)))
      (.added "uploaded" (str (:uploaded file)))
      (.added "filename" (:filename file))
      (.added "content-type" (:content-type file))
      (.added "application-key" (:application-key file))
      (.build)))

(def no-changes (.build (new Changes$Builder)))

(defn user [session ip-str user-agent]
  (User.
    (when-let [oid (-> session :identity :oid)]
      (Oid. oid))
    (or (InetAddress/getByName ip-str)
        (InetAddress/getLocalHost))
    (or (:key session) "no session")
    (or user-agent
        (:user-agent session)
        "no user agent")))

(deftype LoggerAdapter [log-config]
  Logger
  (log [_ str]
    (timbre/log* log-config :info str)))

(defn- audit-log-config [log-path]
  (assoc timbre/example-config
    :appenders {:file-appender
                (assoc (rolling-appender {:path    log-path
                                          :pattern :daily})
                  :output-fn (fn [{:keys [msg_]}] (force msg_)))}))

(defprotocol AuditLog
  (log [this user operation target changes]))

(defrecord AuditLogger [config logger]
  component/Lifecycle

  (start [this]
    (let [log-path (get-in config [:audit-log :path])]
      (when (string/blank? log-path)
        (throw (new RuntimeException "Invalid empty audit log path")))
      (assoc this :logger (Audit. (->LoggerAdapter (audit-log-config log-path))
                                  "liiteri"
                                  ApplicationType/BACKEND))))

  (stop [this]
    (assoc this :logger nil))

  AuditLog

  (log [this user operation target changes]
    (.log logger user operation target changes)))

(defn new-logger []
  (map->AuditLogger {}))
