(ns liiteri.api
  (:require [compojure.api.exception :as ex]
            [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [liiteri.db.file-store :as file-store]
            [liiteri.schema :as schema]
            [liiteri.s3-store :as s3-store]
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

(defn new-api [{:keys [db s3-client]}]
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
        (api/context "/liiteri/api" []
          :tags ["liiteri"]

          (api/POST "/files" []
            :summary "Upload a file"
            :multipart-params [file :- (api/describe upload/TempFileUpload "File to upload")]
            :middleware [upload/wrap-multipart-params]
            :return schema/File
            (try
              (response/ok (s3-store/create-file file s3-client db))
              (finally
                (.delete (:tempfile file)))))

          (api/GET "/files/metadata" []
            :summary "Get metadata for one or more files"
            :query-params [key :- (api/describe [s/Str] "Key of the file")]
            :return [schema/File]
            (let [metadata (file-store/get-metadata key db)]
              (if (> (count metadata) 0)
                (response/ok metadata)
                (response/not-found {:message (str "File with given keys not found")}))))

          (api/GET "/files/:key" []
            :summary "Download a file"
            :path-params [key :- (api/describe s/Str "Key of the file")]
            (if-let [file-stream (s3-store/get-file key s3-client db)]
              (response/ok file-stream)
              (response/not-found)))

          (api/PUT "/files/:key" []
            :summary "Update a file"
            :path-params [key :- (api/describe s/Str "Key of the file")]
            :multipart-params [file :- (api/describe upload/TempFileUpload "File to upload")]
            :middleware [upload/wrap-multipart-params]
            :return schema/File
            (try
              (response/ok (s3-store/update-file file key s3-client db))
              (finally
                (.delete (:tempfile file)))))

          (api/DELETE "/files/:key" []
            :summary "Delete a file"
            :path-params [key :- (api/describe s/Str "Key of the file")]
            :return {:key s/Str}
            (if (> (s3-store/delete-file key s3-client db) 0)
              (response/ok {:key key})
              (response/not-found {:message (str "File with key " key " not found")})))))
      (c/if-url-starts-with "/liiteri/api/" logger-mw/wrap-with-logger)))
