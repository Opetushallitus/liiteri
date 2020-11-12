(ns liiteri.auth.cas-client
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clj-http.client :as http-client]
            [taoensso.timbre :as log])
  (:import [fi.vm.sade.utils.cas CasClient]
           [org.http4s.client.blaze package$]))

(def csrf-value "liiteri")
(def caller-id "1.2.246.562.10.00000000001.liiteri.backend")

(defn enrich-with-mandatory-headers-and-common-settings [opts]
  (-> opts
      (update :connection-timeout (fnil identity 60000))
      (update :socket-timeout (fnil identity 60000))
      (assoc  :throw-exceptions false)
      (update :headers merge
              {"Caller-Id" caller-id}
              {"CSRF" csrf-value})
      (update :cookies merge {"CSRF" {:value csrf-value :path "/"}})))

(defrecord CasClientState [client params session-cookie-name session-id])

(defn new-cas-client [config]
  (new CasClient
       (-> config :virkailija-host)
       (.defaultClient package$/MODULE$)
       caller-id))

(defn- request-with-json-body [request body]
  (-> request
      (assoc-in [:headers "Content-Type"] "application/json")
      (assoc :body (json/generate-string body))))

(defn- create-params [session-cookie-name cas-session-id body]
  (cond-> {:cookies          {session-cookie-name  {:value @cas-session-id :path "/"}}
           :redirect-strategy :none
           :throw-exceptions false}
          (some? body) (request-with-json-body body)))

(defn do-request
  [{:keys [url method] :as opts}]
  (let [method-name (string/upper-case (name method))
        opts        (enrich-with-mandatory-headers-and-common-settings opts)
        start       (System/currentTimeMillis)
        response    (http-client/request opts)
        time        (- (System/currentTimeMillis) start)
        status      (:status response 500)]
    (when (or (<= 400 status) (< 1000 time))
      (log/warn "HTTP" method-name url status (str time "ms")))
    response))

(defn- cas-http [client method url opts & [body]]
  (let [cas-client          (:client client)
        cas-params          (:params client)
        session-cookie-name (:session-cookie-name client)
        cas-session-id      (:session-id client)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name))))
    (let [resp (do-request (merge {:url url :method method}
                                            opts
                                            (create-params session-cookie-name cas-session-id body)))]
      (if (or (= 401 (:status resp))
              (= 302 (:status resp)))
        (do
          (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name)))
          (do-request (merge {:url url :method method}
                                       opts
                                       (create-params session-cookie-name cas-session-id body))))
        resp))))

(defn cas-authenticated-get [client url]
  (cas-http client :get url {}))

(defn cas-authenticated-delete [client url]
  (cas-http client :delete url {}))

(defn cas-authenticated-post [client url body]
  (cas-http client :post url {} body))

(defn cas-authenticated-multipart-post [client url opts]
  (cas-http client :post url opts nil))

(defn cas-authenticated-get-as-stream [client url]
  (cas-http client :get url {:as :stream} nil))