(ns liiteri.api
  (:require [clojure.java.io :as io]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [liiteri.audit-log :as audit-log]
            [liiteri.db.file-metadata-store :as file-metadata-store]
            [liiteri.files.file-store :as file-store]
            [liiteri.schema :as schema]
            [liiteri.mime :as mime]
            [ring.logger.timbre :as logger-mw]
            [ring.middleware.conditional :as c]
            [ring.util.http-response :as response]
            [ring.swagger.upload]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import [ring.swagger.upload Upload]))

(defn- internal-server-error [& _]
  (response/internal-server-error))

(defn- error-logger
  ([response-fn ^Exception e data req]
   (log/error e req)
   (response-fn e data req))
  ([^Exception e data req]
   (error-logger internal-server-error e data req)))

(def ^:private file-extension-blacklist-pattern #"(?i)\.exe$")

(defn- fail-if-file-extension-blacklisted! [filename]
  {:pre [(not (clojure.string/blank? filename))]}
  (when (re-find file-extension-blacklist-pattern filename)
    (throw (IllegalArgumentException. (str "File " filename " has invalid extension")))))

(defn new-api [{:keys [storage-engine db config audit-logger]}]
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
                (let [{:keys [filename tempfile content-type]} file]
                  (fail-if-file-extension-blacklisted! filename)
                  (let [real-file-type (mime/validate-file-content-type! config tempfile filename content-type)
                        resp (file-store/create-file-and-metadata file storage-engine {:connection db})]
                    (audit-log/log audit-logger
                                   (audit-log/unknown-user x-real-ip user-agent)
                                   audit-log/operation-new
                                   (audit-log/file-target (:key resp))
                                   (audit-log/new-file-changes resp))
                    (response/ok resp)))
                (catch IllegalArgumentException e
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
              (let [metadata (file-metadata-store/get-metadata key {:connection db})]
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
              (let [metadata (file-metadata-store/get-metadata keys {:connection db})]
                (if (> (count metadata) 0)
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
              (let [[metadata] (file-metadata-store/get-metadata key {:connection db})]
                (if (= "done" (:virus-scan-status metadata))
                  (if-let [file-response (file-store/get-file key storage-engine {:connection db})]
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
              (let [queue-length (file-metadata-store/get-queue-length {:connection db})
                    {:keys [id key age] :or {age 0}} (file-metadata-store/get-oldest-unscanned-file {:connection db})]
                (response/ok {:unprocessed-queue-length queue-length
                              :oldest-unprocessed-file  {:id  id
                                                         :key key
                                                         :age age}}))))))

      (c/if-url-starts-with "/liiteri/api/" logger-mw/wrap-with-logger)))
