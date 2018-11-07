(ns liiteri.virus-scan-test
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [liiteri.core :as system]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.test-utils :as u]
            [liiteri.virus-scan :as virus-scan]
            [clj-http.client :as http-client]
            [ring.util.http-response :as response])
  (:import [java.io File]
           [java.util UUID]))

(def system (atom (system/new-system {:antivirus {:mock? false :poll-interval-seconds 5}})))
(def metadata (atom nil))
(def file (atom nil))

(defn- init-test-file []
  (let [filename "test-file.txt"
        file-key (str (UUID/randomUUID))
        conn     {:connection (:db @system)}
        base-dir (get-in (:config @system) [:file-store :filesystem :base-path])]
    (with-open [w (io/writer (str base-dir "/" file-key))]
      (.write w "test file\n"))
    (reset! metadata (metadata-store/create-file {:key          file-key
                                                  :filename     filename
                                                  :content-type "text/plain"
                                                  :size         1}
                                                 conn))
    (reset! file (io/file (str base-dir "/" file-key)))))

(defn- remove-test-file []
  (metadata-store/delete-file (:key @metadata) {:connection (:db @system)})
  (io/delete-file @file true))

(use-fixtures :once
  (fn [tests]
    (u/start-system system)
    (u/create-temp-dir system)
    (tests)
    (u/clear-database! system)
    (u/stop-system system)
    (u/remove-temp-dir system)))

(use-fixtures :each
  (fn [tests]
    (init-test-file)
    (tests)
    (remove-test-file)))

(deftest virus-scan-for-clean-file
  (let [db             (:db @system)
        storage-engine (:storage-engine @system)
        config         (:config @system)]
    (with-redefs [http-client/post (fn [& _] (response/ok "Everything ok : true\n"))]
      (#'virus-scan/scan-files db storage-engine config))
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] {:connection db})]
      (is (= (:virus-scan-status metadata) "done")))))

(defn- file-stored? []
  (.exists @file))

(deftest virus-scan-for-contaminated-file
  (let [db             (:db @system)
        storage-engine (:storage-engine @system)
        config         (:config @system)]
    (with-redefs [http-client/post (fn [& _] (response/ok "Everything ok : false\n"))]
      (#'virus-scan/scan-files db storage-engine config))
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] {:connection db})]
      (is (= (:virus-scan-status metadata) "failed"))
      (is (not (file-stored?))))))

(deftest virus-scan-throws-exception
  (let [db             (:db @system)
        storage-engine (:storage-engine @system)
        config         (:config @system)]
    (with-redefs [http-client/post (fn [& _] (throw (Exception. "failed to scan file")))]
      (#'virus-scan/scan-files db storage-engine config))
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] {:connection db})]
      (is (= (:virus-scan-status metadata) "not_started")))))
