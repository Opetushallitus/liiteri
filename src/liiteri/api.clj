(ns liiteri.api
  (:require [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [liiteri.schema :as schema]
            [liiteri.store :as store]
            [ring.util.http-response :as response]))

(defn new-api [{:keys [db]}]
  (api/api {:swagger {:spec    "/liiteri/swagger.json"
                      :ui      "/liiteri/api-docs"
                      :data    {:info {:version     "0.1.0"
                                       :title       "Liiteri API"
                                       :description "File Storage Service API For OPH"}
                                :tags [{:name "liiteri" :description "Liiteri API"}]}
                      :options {:ui {:validatorUrl nil}}}}
    (api/context "/liiteri/api" []
      :tags ["liiteri"]

      (api/POST "/upload" []
        :summary "Upload a file"
        :multipart-params [file :- (api/describe schema/FileUpload "File to upload")]
        :middleware [[upload/wrap-multipart-params {:store (store/stream-store db)}]]
        :return schema/FileUpload
        (response/ok file)))))
