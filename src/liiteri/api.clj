(ns liiteri.api
  (:require [compojure.api.sweet :as api]
            [compojure.api.upload :as upload]
            [ring.util.http-response :as response]))

(defn new-api []
  (api/api {:swagger {:spec         "/liiteri/swagger.json"
                      :ui           "/liiteri/api-docs"
                      :validatorUrl nil
                      :data         {:info {:version     "0.1.0"
                                            :title       "Liiteri API"
                                            :description "File Storage Service API For OPH"}}}}
    (api/context "/liiteri/api" []
      (api/POST "/upload" []
        :multipart-params [file :- upload/TempFileUpload]
        :middleware [upload/wrap-multipart-params]
        (response/ok (dissoc file :tempfile))))))
