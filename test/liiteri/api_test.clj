(ns liiteri.api-test
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [liiteri.config :as config]
            [liiteri.core :as system]
            [liiteri.fixtures :refer :all]
            [liiteri.db.test-metadata-store :as metadata]
            [liiteri.test-utils :as u]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
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
  (doseq [{:keys [filename file-object content-type size]} ok-files]
    (log/info (format "Testing normal file upload for filename %s with content-type %s, size %d (should pass)" filename content-type size))
    (let [path (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
          resp @(http/post path {:multipart [{:name "file" :content file-object :filename filename :content-type content-type}]})
          body (json/parse-string (:body resp) true)]
      (is (= (:status resp) 200))
      (is (file-stored? (:key body)))
      (is (= (:filename body) filename))
      (is (= (:content-type body) content-type))
      (is (= (:size body) size))
      (is (= (:deleted body) nil))
      (is (= (:final body) false))
      (is (some? (:uploaded body)))
      (let [saved-metadata (metadata/get-metadata-for-tests [(:key body)] {:connection (:db @system)})]
        (is (= (:filename saved-metadata) filename))
        (is (= (:size saved-metadata) size))
        (is (nil? (:deleted saved-metadata)))
        (is (= "not_started" (:virus-scan-status saved-metadata))))
      (let [_              @(http/post (str path "/finalize")
                                       {:headers {"Content-Type" "application/json"}
                                        :body    (json/generate-string {:keys [(:key body)]})})
            saved-metadata (metadata/get-metadata-for-tests [(:key body)] {:connection (:db @system)})]
        (is (= (:final saved-metadata) true)))
      (let [delete-resp @(http/delete (str path "/" (:key body)))]
        (is (= (:status delete-resp) 200))
        (is (= (json/parse-string (:body delete-resp) true) {:key (:key body)}))))))

(deftest mangled-extensions
  (doseq [{:keys [mangled-filename filename file-object content-type size]} mangled-extension-files]
    (log/info (format "Testing extension repairing for filename %s -> %s with content-type %s, size %d (should pass)"
                      mangled-filename
                      filename
                      content-type
                      size))
    (let [path (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
          resp @(http/post path {:multipart [{:name "file" :content file-object :filename mangled-filename :content-type content-type}]})
          body (json/parse-string (:body resp) true)]
      (is (= (:status resp) 200))
      (is (file-stored? (:key body)))
      (is (= (:filename body) filename))
      (is (= (:content-type body) content-type))
      (is (= (:size body) size))
      (is (= (:deleted body) nil))
      (is (= (:final body) false))
      (is (some? (:uploaded body)))
      (let [saved-metadata (metadata/get-metadata-for-tests [(:key body)] {:connection (:db @system)})]
        (is (= (:filename saved-metadata) filename))
        (is (= (:size saved-metadata) size))
        (is (nil? (:deleted saved-metadata)))
        (is (= "not_started" (:virus-scan-status saved-metadata))))
      (let [_              @(http/post (str path "/finalize")
                                       {:headers {"Content-Type" "application/json"}
                                        :body    (json/generate-string {:keys [(:key body)]})})
            saved-metadata (metadata/get-metadata-for-tests [(:key body)] {:connection (:db @system)})]
        (is (= (:final saved-metadata) true)))
      (let [delete-resp @(http/delete (str path "/" (:key body)))]
        (is (= (:status delete-resp) 200))
        (is (= (json/parse-string (:body delete-resp) true) {:key (:key body)}))))))

(deftest forbidden-files-refused
  (doseq [{:keys [filename file-object content-type size]} forbidden-files]
    (log/info (format "Testing %s with content-type %s, size %d (should fail)" filename content-type size))
    (let [path (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
          resp @(http/post path {:multipart [{:name "file" :content file-object :filename filename :content-type content-type}]})
          body (json/parse-string (:body resp) true)]
      (is (= (:status resp) 400))
      (is (nil? body))
      (let [saved-metadata (metadata/get-metadata-for-tests [(:key body)] {:connection (:db @system)})]
        (is (nil? saved-metadata))))))

(deftest virus-download
  (let [file           (io/file (io/resource "test-files/virus.txt"))
        path           (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
        upload-resp    @(http/post path {:multipart [{:name "file" :content file :filename "virus.txt" :content-type "image/png"}]})
        key            (:key (json/parse-string (:body upload-resp) true))
        _              (metadata/finalize-files key {:connection (:db @system)})
        _              (.scan-files! (:virus-scan @system))
        saved-metadata (metadata/get-metadata-for-tests [key] {:connection (:db @system)})
        download-resp  @(http/get (str path "/" key))]
    (is (= "virus_found" (:virus-scan-status saved-metadata)))
    (is (= (:status download-resp) 404))))
