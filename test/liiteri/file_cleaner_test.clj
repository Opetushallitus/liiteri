(ns liiteri.file-cleaner-test
  (:require [clj-time.core :as t]
            [clojure.test :refer :all]
            [liiteri.core :as system]
            [liiteri.files.file-store :as file-store]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.file-cleaner :as cleaner]
            [liiteri.test-utils :as u])
  (:import [java.util UUID]
           [java.sql Timestamp]))

(def system (atom (system/new-system true)))
(def metadata (atom nil))

(use-fixtures :once
              (fn [tests]
                (u/start-system system)
                (tests)
                (u/clear-database! system)
                (u/stop-system system)))

(defn- init-test-file [uploaded]
  (let [filename "test-file.txt"
        file-key (str (UUID/randomUUID))
        application-key "1.2.246.562.11.000000000000000000001"
        origin-system "Test-system"
        conn {:connection (:db @system)}
        store (:storage-engine @system)]
    (file-store/create-file-from-bytearray store (.getBytes "test file") file-key)
    (reset! metadata (test-metadata-store/create-file {:key             file-key
                                                       :filename        filename
                                                       :content-type    "text/plain"
                                                       :size            1
                                                       :uploaded        uploaded
                                                       :origin-system   origin-system
                                                       :origin-reference application-key}
                                                      conn))))

(defn- remove-test-file []
  (metadata-store/delete-file (:key @metadata) "file-cleaner-test" {:connection (:db @system)} false)
  (file-store/delete-file (:storage-engine @system) (:key @metadata)))

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
    (#'cleaner/clean-files db storage-engine)
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] {:connection db})]
      (is (some? (:deleted metadata)))
      ; dockerized test db time may differ from test host clock, so disable:
      ; (is (t/before? (:deleted metadata) (t/now)))

      ;(is (not (.exists (File. (.getPath @file)))))
      (is (not (file-store/file-exists? storage-engine (:key metadata)))))))

(deftest file-cleaner-does-not-remove-file
  (let [db             (:db @system)
        storage-engine (:storage-engine @system)
        config         (:config @system)
        uploaded       (-> (t/now)
                           (.getMillis)
                           (Timestamp.))]
    (init-test-file uploaded)
    (#'cleaner/clean-files db storage-engine)
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] {:connection db})]
      (is (nil? (:deleted metadata)))

      ;(is (.exists (File. (.getPath @file))))
      (is (file-store/file-exists? storage-engine (:key metadata)))

      )))
