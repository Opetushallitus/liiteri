(ns liiteri.file-cleaner-test
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [liiteri.core :as system]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.file-cleaner :as cleaner]
            [liiteri.test-utils :as u])
  (:import [java.util UUID]
           [java.sql Timestamp]
           [java.io File]))

(def system (atom (system/new-system)))
(def metadata (atom nil))
(def file (atom nil))

(use-fixtures :once
  (fn [tests]
    (u/start-system system)
    (u/create-temp-dir system)
    (tests)
    (u/clear-database! system)
    (u/stop-system system)
    (u/remove-temp-dir system)))

(defn- init-test-file [uploaded]
  (jdbc/with-db-transaction [tx (:db @system)]
    (let [filename "test-file.txt"
          file-key (str (UUID/randomUUID))
          base-dir (get-in (:config @system) [:file-store :filesystem :base-path])]
      (with-open [w (io/writer (str base-dir "/" file-key))]
        (.write w "test file\n"))
      (reset! metadata (test-metadata-store/create-file {:key          file-key
                                                         :filename     filename
                                                         :content-type "text/plain"
                                                         :size         1
                                                         :uploaded     uploaded}
                                                        tx))
      (reset! file (io/file (str base-dir "/" file-key))))))

(defn- remove-test-file []
  (metadata-store/delete-file (:key @metadata) (:db @system))
  (io/delete-file @file true))

(use-fixtures :each
  (fn [tests]
    (tests)
    (remove-test-file)))

(deftest file-cleaner-removes-file
  (let [db             (:db @system)
        storage-engine (:storage-engine @system)
        config         (:config @system)
        uploaded       (-> (t/now)
                           (t/minus (t/months 1))
                           (.getMillis)
                           (Timestamp.))]
    (init-test-file uploaded)
    (#'cleaner/clean-files db storage-engine config)
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] db)]
      (is (some? (:deleted metadata)))
      (is (t/before? (:deleted metadata) (t/now)))
      (is (not (.exists (File. (.getPath @file))))))))

(deftest file-cleaner-does-not-remove-file
  (let [db             (:db @system)
        storage-engine (:storage-engine @system)
        config         (:config @system)
        uploaded       (-> (t/now)
                           (.getMillis)
                           (Timestamp.))]
    (init-test-file uploaded)
    (#'cleaner/clean-files db storage-engine config)
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] db)]
      (is (nil? (:deleted metadata)))
      (is (.exists (File. (.getPath @file)))))))
