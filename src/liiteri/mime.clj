(ns liiteri.mime
  (:require [taoensso.timbre :as log]
            [ring.util.http-response :as http-response]
            [pantomime.mime :as mime]))

(defn validate-file-content-type! [config file filename provided-content-type]
  (let [allowed-mime-types (-> config :file-store :attachment-mime-types)
        real-content-type (mime/mime-type-of file)]
    (if (not-any? (partial = real-content-type) allowed-mime-types)
      (do
        (log/warn (str "Request with illegal content-type '" real-content-type "' of file '" filename "' (provided '" provided-content-type "' ). Allowed: " allowed-mime-types)
                  (http-response/bad-request! {:provided-content-type provided-content-type
                                               :illegal-content-type real-content-type
                                               :allowed-content-types allowed-mime-types})))
      real-content-type)))

