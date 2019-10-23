(ns liiteri.preview.preview-generator-test
  (:require [clojure.test :refer :all]
            [liiteri.core :as system]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.files.file-store :as file-store]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.fixtures :as fixtures]
            [liiteri.test-utils :as u]
            [liiteri.preview.preview-generator :as preview-generator]
            [com.stuartsierra.component :as component]
            [clj-time.core :as t])
  (:import [java.sql Timestamp]))

(def system (atom (system/new-system {})))

(use-fixtures :once
              (fn [tests]
                (u/start-system system)
                (u/create-temp-dir system)
                (component/stop (:preview-generator @system))
                (tests)
                (u/clear-database! system)
                (u/stop-system system)
                (u/remove-temp-dir system)))

(defn- assert-has-single-png-preview-page [file-metadata previews]
  (is (= 1 (:page-count file-metadata)))
  (is (= "finished" (:preview-status file-metadata)))
  (is (= 1 (count (:previews file-metadata))))

  (is (= 1 (count previews)))
  (is (= (str (:filename file-metadata) ".0" (-> (first previews) :key))))
  (is (= "image/png" (-> (first previews) :content-type)))
  (is (< 0 (-> (first previews) :size))))

(defn- assert-has-no-previews [file-metadata previews]
  (is (= nil (:page-count file-metadata)))
  (is (= "not_supported" (:preview-status file-metadata)))
  (is (= 0 (count (:previews file-metadata))))

  (is (= 0 (count previews))))

(deftest previews-are-generated-for-pdf-files
  (let [store (u/new-in-memory-store)
        conn  {:connection (:db @system)}]
    (doseq [[filename file content-type size] fixtures/ok-files]
      (let [uploaded  (-> (t/now)
                          (.getMillis)
                          (Timestamp.))
            file-spec {:key          filename
                       :filename     filename
                       :content-type content-type
                       :size         size
                       :uploaded     uploaded}]
        (test-metadata-store/create-file file-spec conn)
        (file-store/create-file store file filename)
        (metadata-store/set-virus-scan-status! filename "done" conn)
        (metadata-store/finalize-files [filename] conn)
        (preview-generator/generate-file-previews (:config @system) conn store file-spec)

        (let [file-metadata-after-preview (first (metadata-store/get-metadata [filename] conn))
              generated-previews          (vec (metadata-store/get-previews filename conn))]
          (if (= "application/pdf" content-type)
            (assert-has-single-png-preview-page file-metadata-after-preview generated-previews)
            (assert-has-no-previews file-metadata-after-preview generated-previews)))))))
