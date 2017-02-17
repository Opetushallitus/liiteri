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
        :multipart-params [file :- (api/describe (Upload. schema/File) "File to upload")]
        :middleware [[upload/wrap-multipart-params {:store (s3-store/s3-store s3-client db)}]]
        :return schema/File
        (response/ok file))

      (api/DELETE "/files/:id" []
        :summary "Delete a file"
        :path-params [id :- s/Int]
        :return schema/File
        (response/ok (s3-store/delete-file id s3-client db))))))
