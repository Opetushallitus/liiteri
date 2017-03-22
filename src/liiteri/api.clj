(ns liiteri.api
  (:require [clojure.java.io :as io]
            [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [liiteri.db.file-metadata-store :as file-metadata-store]
            [liiteri.schema :as schema]
            [liiteri.files.s3.s3-store :as s3-store]
            [ring.util.http-response :as response]
            [ring.swagger.upload]
            [schema.core :as s])
  (:import [ring.swagger.upload Upload]))

(defn new-api [{:keys [file-store]}]
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
          (response/ok (.create-file file-store file))
          (finally
            (io/delete-file (:tempfile file) true))))

      (api/GET "/files/metadata" []
        :summary "Get metadata for one or more files"
        :query-params [key :- (api/describe [s/Str] "Key of the file")]
        :return [schema/File]
        (let [metadata (file-metadata-store/get-metadata key)]
          (if (> (count metadata) 0)
            (response/ok metadata)
            (response/not-found {:message (str "File with given keys not found")}))))

      (api/GET "/files/:key" []
        :summary "Download a file"
        :path-params [key :- (api/describe s/Str "Key of the file")]
        (if-let [file-stream (.get-file file-store key)]
          (response/ok file-stream)
          (response/not-found)))

      (api/PUT "/files/:key" []
        :summary "Update a file"
        :path-params [key :- (api/describe s/Str "Key of the file")]
        :multipart-params [file :- (api/describe upload/TempFileUpload "File to upload")]
        :middleware [upload/wrap-multipart-params]
        :return schema/File
        (try
          (response/ok (.update-file file-store file key))
          (finally
            (io/delete-file (:tempfile file) true))))

      (api/DELETE "/files/:key" []
        :summary "Delete a file"
        :path-params [key :- (api/describe s/Str "Key of the file")]
        :return {:key s/Str}
        (if (> (.delete-file file-store key) 0)
          (response/ok {:key key})
          (response/not-found {:message (str "File with key " key " not found")}))))))
