(ns liiteri.api
  (:require [clj-access-logging]
            [clj-stdout-access-logging]
            [clj-timbre-access-logging]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [environ.core :refer [env]]
            [liiteri.auth.session-timeout :as session-timeout]
            [clj-ring-db-session.session.session-client :as session-client]
            [clj-ring-db-session.authentication.auth-middleware :as crdsa-auth-middleware]
            [ring.middleware.session :as ring-session]
            [liiteri.audit-log :as audit-log]
            [liiteri.db.file-metadata-store :as file-metadata-store]
            [liiteri.files.file-store :as file-store]
            [liiteri.schema :as schema]
            [liiteri.mime :as mime]
            [ring.util.http-response :as response]
            [ring.swagger.upload]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(defn- create-wrap-database-backed-session [session-store]
  (fn [handler]
    (ring-session/wrap-session handler
                               {:root         "/liiteri"
                                :cookie-attrs {:secure (not (:dev? env))}
                                :store        session-store})))

(defn- internal-server-error [& _]
  (response/internal-server-error))

(defn- error-logger
  ([response-fn ^Exception e data req]
   (log/error e)
   (response-fn e data req))
  ([^Exception e data req]
   (error-logger internal-server-error e data req)))

(def ^:private file-extension-blacklist-pattern #"(?i)\.exe$")

(defn- fail-if-file-extension-blacklisted! [filename]
  {:pre [(not (string/blank? filename))]}
  (when (re-find file-extension-blacklist-pattern filename)
    (throw (IllegalArgumentException. (str "File " filename " has invalid extension")))))

(defn api-routes [{:keys [storage-engine db config audit-logger]}]
  (api/context "/api" []
    :tags ["liiteri"]

    (api/POST "/files" []
      :summary "Upload a file"
      :header-params [{x-real-ip :- s/Str nil}
                      {user-agent :- s/Str nil}]
      :multipart-params [file :- (api/describe upload/TempFileUpload "File to upload")]
      :middleware [upload/wrap-multipart-params]
      :return schema/File
      (try
        (let [{:keys [filename tempfile content-type size]} file]
          (fail-if-file-extension-blacklisted! filename)
          (let [resp (-> (mime/file->validated-file-spec! config filename tempfile size content-type)
                         (file-store/create-file-and-metadata storage-engine {:connection db}))]
            (audit-log/log audit-logger
                           (audit-log/unknown-user x-real-ip user-agent)
                           audit-log/operation-new
                           (audit-log/file-target (:key resp))
                           (audit-log/new-file-changes resp))
            (response/ok resp)))
        (catch IllegalArgumentException e
          (log/warn (format "File failed upload validation: %s", (.getMessage e)))
          (response/bad-request! (get-in (ex-data e) [:response :body])))
        (finally
          (io/delete-file (:tempfile file) true))))

    (api/POST "/files/finalize" []
      :summary "Finalize one or more files"
      :header-params [{x-real-ip :- s/Str nil}
                      {user-agent :- s/Str nil}]
      :body-params [keys :- [s/Str]]
      (when (> (count keys) 0)
        (file-metadata-store/finalize-files keys {:connection db})
        (doseq [key keys]
          (audit-log/log audit-logger
                         (audit-log/unknown-user x-real-ip user-agent)
                         audit-log/operation-finalize
                         (audit-log/file-target key)
                         audit-log/no-changes)))
      (response/ok))

    (api/GET "/files/metadata" []
      :summary "Get metadata for one or more files"
      :query-params [key :- (api/describe [s/Str] "Key of the file")]
      :header-params [{x-real-ip :- s/Str nil}
                      {user-agent :- s/Str nil}]
      :return [schema/File]
      (let [metadata (file-metadata-store/get-normalized-metadata! key {:connection db})]
        (if (> (count metadata) 0)
          (do (doseq [{:keys [key]} metadata]
                (audit-log/log audit-logger
                               (audit-log/unknown-user x-real-ip user-agent)
                               audit-log/operation-metadata-query
                               (audit-log/file-target key)
                               audit-log/no-changes))
              (response/ok metadata))
          (response/not-found {:message (str "File with given keys not found")}))))

    (api/POST "/files/metadata" []
      :summary "Get metadata for one or more files"
      :body-params [keys :- (api/describe [s/Str] "Keys of the files")]
      :header-params [{x-real-ip :- s/Str nil}
                      {user-agent :- s/Str nil}]
      :return [schema/File]
      (let [metadata (file-metadata-store/get-normalized-metadata! keys {:connection db})]
        (if (= (count metadata) (count keys))
          (do (doseq [{:keys [key]} metadata]
                (audit-log/log audit-logger
                               (audit-log/unknown-user x-real-ip user-agent)
                               audit-log/operation-metadata-query
                               (audit-log/file-target key)
                               audit-log/no-changes))
              (response/ok metadata))
          (response/not-found {:message (str "Files with given keys not found")}))))

    (api/GET "/files/:key" []
      :summary "Download a file"
      :header-params [{x-real-ip :- s/Str nil}
                      {user-agent :- s/Str nil}]
      :path-params [key :- (api/describe s/Str "Key of the file")]
      (let [[metadata] (file-metadata-store/get-normalized-metadata! [key] {:connection db})]
        (if (= "done" (:virus-scan-status metadata))
          (if-let [file-response (file-store/get-file-and-metadata key storage-engine {:connection db})]
            (do (audit-log/log audit-logger
                               (audit-log/unknown-user x-real-ip user-agent)
                               audit-log/operation-file-query
                               (audit-log/file-target key)
                               audit-log/no-changes)
                (-> (response/ok (:body file-response))
                    (response/header
                      "Content-Disposition"
                      (str "attachment; filename=\"" (:filename file-response) "\""))))
            (response/not-found))
          (response/not-found))))

    (api/DELETE "/files/:key" []
      :summary "Delete a file"
      :header-params [{x-real-ip :- s/Str nil}
                      {user-agent :- s/Str nil}]
      :path-params [key :- (api/describe s/Str "Key of the file")]
      :return {:key s/Str}
      (let [deleted-count (file-store/delete-file-and-metadata key storage-engine {:connection db})]
        (if (> deleted-count 0)
          (do (audit-log/log audit-logger
                             (audit-log/unknown-user x-real-ip user-agent)
                             audit-log/operation-delete
                             (audit-log/file-target key)
                             audit-log/no-changes)
              (response/ok {:key key}))
          (response/not-found {:message (str "File with key " key " not found")}))))

    (api/GET "/queue-status" []
      :summary "Display virus scan file queue status metrics"
      :return {:unprocessed-queue-length s/Int
               :oldest-unprocessed-file  {:id  (s/maybe s/Int)
                                          :key (s/maybe s/Str)
                                          :age s/Int}}
      (let [queue-length    (file-metadata-store/get-queue-length {:connection db})
            {:keys [id key age] :or {age 0}} (file-metadata-store/get-oldest-unscanned-file {:connection db})
            status-ok?      (and (< queue-length 100) (< age 3600))
            response-status (if status-ok? response/ok response/internal-server-error)]
        (response-status {:unprocessed-queue-length queue-length
                          :oldest-unprocessed-file  {:id  id
                                                     :key key
                                                     :age age}})))))

(defn new-api [{:keys [config session-store] :as this}]
  (-> (api/api {:swagger    {:spec    "/liiteri/swagger.json"
                             :ui      "/liiteri/api-docs"
                             :data    {:info {:version     "0.1.0"
                                              :title       "Liiteri API"
                                              :description "File Storage Service API For OPH"}
                                       :tags [{:name "liiteri" :description "Liiteri API"}]}
                             :options {:ui {:validatorUrl nil}}}
                :exceptions {:handlers {::ex/request-parsing     (ex/with-logging (partial error-logger ex/request-parsing-handler) :warn)
                                        ::ex/request-validation  (ex/with-logging (partial error-logger ex/request-validation-handler) :warn)
                                        ::ex/response-validation (ex/with-logging (partial error-logger ex/response-validation-handler) :error)
                                        ::ex/default             (ex/with-logging error-logger :error)}}}

               (api/context "/liiteri" []
                 (api/undocumented
                   (api/GET "/buildversion.txt" []
                     (response/ok (slurp (io/resource "buildversion.txt")))))

                 (api/middleware
                   [(create-wrap-database-backed-session session-store)
                    (when-not (:dev? env)
                      #(crdsa-auth-middleware/with-authentication %
                                                                  (str (-> config :virkailija-host) "/cas/login")))]
                   (api/middleware [session-client/wrap-session-client-headers
                                    (session-timeout/wrap-idle-session-timeout config)]
                     (api-routes this)))))
      (clj-access-logging/wrap-access-logging)
      (clj-stdout-access-logging/wrap-stdout-access-logging)
      (clj-timbre-access-logging/wrap-timbre-access-logging
       {:path (str (get-in config [:access-log :path])
                   (when (:hostname env) (str "_" (:hostname env))))})))
