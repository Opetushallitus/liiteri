(ns liiteri.api
  (:require [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [liiteri.schema :as schema]
            [liiteri.s3-store :as s3-store]
            [ring.util.http-response :as response]
            [ring.swagger.upload]
            [schema.core :as s])
  (:import [ring.swagger.upload Upload]))

(defn new-api [{:keys [db s3-client]}]
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
          (response/not-found {:message (str "File with key " key " not found")}))))))
