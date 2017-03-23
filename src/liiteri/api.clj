(ns liiteri.api
  (:require [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [liiteri.db.file-store :as file-store]
            [liiteri.schema :as schema]
            [liiteri.s3-store :as s3-store]
            [liiteri.av :as av]
            [ring.util.http-response :as response]
            [ring.swagger.upload]
            [schema.core :as s])
  (:import [ring.swagger.upload Upload]))

(defn new-api [{:keys [db s3-client av]}]
  (api/api {:swagger {:spec    "/liiteri/swagger.json"
                      :ui      "/liiteri/api-docs"
                      :data    {:info {:version     "0.1.0"
                                       :title       "Liiteri API"
                                       :description "File Storage Service API For OPH"}
                                :tags [{:name "liiteri" :description "Liiteri API"}]}
                      :options {:ui {:validatorUrl nil}}}}
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
        (if (> (s3-store/delete-file key "API" s3-client db) 0)
          (response/ok {:key key})
          (response/not-found {:message (str "File with key " key " not found")})))

      (api/GET "/files" []
        :summary "Get metadata for one or more files"
        :query-params [key :- (api/describe [s/Str] "Key of the file")]
        :return [schema/File]
        (let [metadata (file-store/get-metadata key db)]
          (if (> (count metadata) 0)
            (response/ok metadata)
            (response/not-found {:message (str "File with given keys not found")}))))

      (api/GET "/av" []
        :summary "Execute virus check for db files"
        :return {}
        (response/ok (av/check-db-files db s3-client)))

      (api/POST "/av" []
        :summary "Check file for viruses"
        :multipart-params [file :- (api/describe upload/TempFileUpload "File to upload")]
        :middleware [upload/wrap-multipart-params]
        (try
          (response/ok (av/check-multipart-file file))
          (finally
            (.delete (:tempfile file))))))))
