(ns liiteri.api
  (:require [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [liiteri.store :as store]
            [ring.util.http-response :as response]))

(defn new-api []
  (api/api {:swagger {:spec         "/liiteri/swagger.json"
                      :ui           "/liiteri/api-docs"
                      :validatorUrl nil
                      :data         {:info {:version     "0.1.0"
                                            :title       "Liiteri API"
                                            :description "File Storage Service API For OPH"}
                                     :tags [{:name "liiteri" :description "Liiteri API"}]}}}
    (api/context "/liiteri/api" []
      :tags ["liiteri"]

      (api/POST "/upload" []
        :summary "Upload a file"
        :multipart-params [stream :- (api/describe store/StreamUpload "File to upload")]
        :middleware [[upload/wrap-multipart-params {:store (store/stream-store)}]]
        (response/ok {})))))
