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
              :multipart-params [file :- (api/describe upload/TempFileUpload "File to upload")]
              :middleware [upload/wrap-multipart-params]
              :return schema/File
              (try
                (fail-if-file-extension-blacklisted! (:filename file))
                (let [real-file-type (mime/validate-file-content-type config (:tempfile file) (:filename file) (:content-type file))
                      fixed-filename (mime/file-name-according-to-content-type (:filename file) real-file-type)
                      {:keys [key] :as resp} (file-store/create-file (assoc file :filename fixed-filename) storage-engine db)]
                  (.log audit-logger key audit-log/operation-new resp)
                  (response/ok resp))
                (catch Exception e
                  (let [error (ex-data e)]
                    (.log audit-logger "" audit-log/operation-new (:response error))
                    (response/bad-request! (-> error :response :body))))
                (finally
                  (io/delete-file (:tempfile file) true))))

            (api/POST "/files/finalize" []
              :summary "Finalize one or more files"
              :query-params [keys :- [s/Str]]
              (clojure.pprint/pprint keys)
              (doseq [key keys]
                (file-metadata-store/finalize-file key db))
              (response/ok))

            (api/GET "/files/metadata" []
              :summary "Get metadata for one or more files"
              :query-params [key :- (api/describe [s/Str] "Key of the file")]
              :return [schema/File]
              (let [metadata (file-metadata-store/get-metadata key db)]
                (.log audit-logger key audit-log/operation-query metadata)
                (if (> (count metadata) 0)
                  (response/ok metadata)
                  (response/not-found {:message (str "File with given keys not found")}))))

            (api/GET "/files/:key" []
              :summary "Download a file"
              :path-params [key :- (api/describe s/Str "Key of the file")]
              (let [[metadata] (file-metadata-store/get-metadata key db)]
                (.log audit-logger key audit-log/operation-query metadata)
                (if (= "done" (:virus-scan-status metadata))
                  (if-let [file-response (file-store/get-file key storage-engine db)]
                    (-> (response/ok (:body file-response))
                        (response/header
                          "Content-Disposition"
                          (str "attachment; filename=\"" (:filename file-response) "\"")))
                    (response/not-found))
                  (response/not-found))))

            (api/DELETE "/files/:key" []
              :summary "Delete a file"
              :path-params [key :- (api/describe s/Str "Key of the file")]
              :return {:key s/Str}
              (let [deleted-count (file-store/delete-file key storage-engine db)]
                (.log audit-logger key audit-log/operation-delete {:deleted-count deleted-count})
                (if (> deleted-count 0)
                  (response/ok {:key key})
                  (response/not-found {:message (str "File with key " key " not found")})))))))
      (c/if-url-starts-with "/liiteri/api/" logger-mw/wrap-with-logger)))
