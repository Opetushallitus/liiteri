(ns liiteri.api-test
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [liiteri.config :as config]
            [liiteri.core :as system]
            [liiteri.db.test-metadata-store :as metadata]
            [liiteri.test-utils :as u]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]))

(def system (atom nil))
(def config (config/new-config))

(use-fixtures :once
  (fn [tests]
    (u/start-system system)
    (u/create-temp-dir system)
    (tests)
    (u/clear-database! system)
    (u/stop-system system)
    (u/remove-temp-dir system)))

(defn- file-stored? [file-key]
  (let [file (io/file (str (get-in config [:file-store :filesystem :base-path]) "/" file-key))]
    (.exists file)))

(deftest file-upload
  (let [file (io/file (io/resource "parrot.png"))
        path (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
        resp @(http/post path {:multipart [{:name "file" :content file :filename "parrot.png" :content-type "image/png"}]})
        body (json/parse-string (:body resp) true)]
    (is (= (:status resp) 200))
    (is (file-stored? (:key body)))
    (is (= (:filename body) "parrot.png"))
    (is (= (:content-type body) "image/png"))
    (is (= (:size body) 7777))
    (is (= (:deleted body) nil))
    (is (= (:final body) false))
    (is (some? (:uploaded body)))
    (let [saved-metadata (metadata/get-metadata-for-tests [(:key body)] (:db @system))]
      (is (= (:filename saved-metadata) "parrot.png"))
      (is (= (:size saved-metadata) 7777))
      (is (nil? (:deleted saved-metadata)))
      (is (= "not_started" (:virus-scan-status saved-metadata))))
    (let [resp           @(http/post (str path "/finalize")
                                     {:query-params {:keys [(:key body)]}})
          saved-metadata (metadata/get-metadata-for-tests [(:key body)] (:db @system))]
      (is (= (:final saved-metadata) true)))))

(deftest exe-extension-refused
  (let [file (io/file (io/resource "parrot.png"))
        path (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
        resp @(http/post path {:multipart [{:name "file" :content file :filename "parrot.exe" :content-type "image/png"}]})
        body (json/parse-string (:body resp) true)]
    (is (= (:status resp) 400))
    (is (nil? body))
    (let [saved-metadata (metadata/get-metadata-for-tests [(:key body)] (:db @system))]
      (is (nil? saved-metadata)))))

(deftest virus-download
  (let [file (io/file (io/resource "virus.txt"))
        path (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
        upload-resp @(http/post path {:multipart [{:name "file" :content file :filename "virus.txt" :content-type "image/png"}]})
        key (:key (json/parse-string (:body upload-resp) true))
        _ (.scan-files! (:virus-scan @system))
        saved-metadata (metadata/get-metadata-for-tests [key] (:db @system))
        download-resp @(http/get (str path "/" key))]
    (is (= "failed" (:virus-scan-status saved-metadata)))
    (is (= (:status download-resp) 404))))
