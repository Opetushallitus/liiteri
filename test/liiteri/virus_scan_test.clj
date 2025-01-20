(ns liiteri.virus-scan-test
  (:require [clojure.test :refer :all]
            [liiteri.core :as system]
            [liiteri.files.file-store :as file-store]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.test-utils :as u]
            [liiteri.virus-scan :as virus-scan])
  (:import [java.util UUID]))

(def system (atom (system/new-system false {})))
(def metadata (atom nil))

(defn- init-test-file []
  (let [filename "test-file.txt"
        file-key (str (UUID/randomUUID))
        origin-system "Test-system"
        origin-reference "1.2.246.562.11.000000000000000000001"
        conn {:connection (:db @system)}
        store (:storage-engine @system)]
    (file-store/create-file-from-bytearray store (.getBytes "test file") file-key)
    (reset! metadata (metadata-store/create-file {:key              file-key
                                                  :filename         filename
                                                  :content-type     "text/plain"
                                                  :size             1
                                                  :origin-system    origin-system
                                                  :origin-reference origin-reference}
                                                 conn))
    (metadata-store/finalize-files file-key origin-system origin-reference conn)))

(defn- remove-test-file []
  (metadata-store/delete-file (:key @metadata) "virus-scan-test" {:connection (:db @system)} false)
  (file-store/delete-file (:storage-engine @system) (:key @metadata)))

(use-fixtures :once
              (fn [tests]
                (u/start-system system)
                (tests)
                (u/clear-database! system)
                (u/stop-system system)))

(use-fixtures :each
              (fn [tests]
                (init-test-file)
                (tests)
                (remove-test-file)))

(defn- wait-for-status [status get-metadata]
  (is (loop [virus-scan-status ""
             retries 20]
        (Thread/sleep 500)
        (if (= virus-scan-status status)
          true
          (if (zero? retries)
            false
            (recur (:virus-scan-status (get-metadata)) (dec retries)))))))

(deftest virus-scan-for-clean-file
  (let [db             (:db @system)
        virus-scan     (:virus-scan @system)
        get-metadata #(test-metadata-store/get-metadata-for-tests [(:key @metadata)] {:connection db})
        {:keys [key filename content-type]} (get-metadata)
        store (:storage-engine @system)]
    (virus-scan/request-file-scan virus-scan [{:key key :filename filename :content-type content-type}])
    (wait-for-status "done" get-metadata)
    (is (file-store/file-exists? store key))))

(deftest virus-scan-for-contaminated-file
  (let [db             (:db @system)
        virus-scan     (:virus-scan @system)
        get-metadata #(test-metadata-store/get-metadata-for-tests [(:key @metadata)] {:connection db})
        {:keys [key content-type]} (get-metadata)
        store (:storage-engine @system)]
    (virus-scan/request-file-scan virus-scan [{:key key :filename "eicar" :content-type content-type}])
    (wait-for-status "virus_found" get-metadata)
    ; objects that fail virus scan get deleted
    (is (not (file-store/file-exists? store key)))))