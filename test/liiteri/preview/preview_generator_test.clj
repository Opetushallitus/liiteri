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

(use-fixtures :each
              (fn [tests]
                (u/start-system system)
                (component/stop (:preview-generator @system))
                (tests)
                (u/clear-database! system)
                (u/stop-system system)))

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

(defn- assert-preview-generation-errored [file-metadata previews]
  (is (= nil (:page-count file-metadata)))
  (is (= "error" (:preview-status file-metadata)))
  (is (= 0 (count (:previews file-metadata))))

  (is (= 0 (count previews))))

(deftest previews-are-generated-for-pdf-files
  (let [store (u/new-in-memory-store)
        conn  {:connection (:db @system)}]
    (doseq [{:keys [filename file-object content-type size]} fixtures/ok-files]
      (let [uploaded  (-> (t/now)
                          (.getMillis)
                          (Timestamp.))
            origin-system "Test-system"
            origin-reference "1.2.246.562.11.000000000000000000001"
            file-spec {:key              filename
                       :filename         filename
                       :content-type     content-type
                       :size             size
                       :uploaded         uploaded
                       :origin-system    origin-system
                       :origin-reference origin-reference}]
        (test-metadata-store/create-file file-spec conn)
        (file-store/create-file store file-object filename)
        (metadata-store/set-virus-scan-status! filename "done" conn)
        (metadata-store/finalize-files [filename] origin-system origin-reference conn)
        (preview-generator/generate-file-previews (:config @system) conn store file-spec)

        (let [file-metadata-after-preview (first (metadata-store/get-normalized-metadata! [filename] conn))
              generated-previews          (vec (metadata-store/get-previews filename conn))]
          (if (= "application/pdf" content-type)
            (assert-has-single-png-preview-page file-metadata-after-preview generated-previews)
            (assert-has-no-previews file-metadata-after-preview generated-previews)))))))

(deftest preview-generation-fails-gracefully-after-timeout
  (let [store (u/new-in-memory-store)
        conn  {:connection (:db @system)}]
    (doseq [{:keys [filename file-object content-type size]} fixtures/not-ok-preview-files]
      (let [uploaded  (-> (t/now)
                          (.getMillis)
                          (Timestamp.))
            origin-system "Test-system"
            origin-reference "1.2.246.562.11.000000000000000000001"
            file-spec {:key              filename
                       :filename         filename
                       :content-type     content-type
                       :size             size
                       :uploaded         uploaded
                       :origin-system    origin-system
                       :origin-reference origin-reference}]
        (test-metadata-store/create-file file-spec conn)
        (file-store/create-file store file-object filename)
        (metadata-store/set-virus-scan-status! filename "done" conn)
        (metadata-store/finalize-files [filename] origin-system origin-reference conn)
        (preview-generator/generate-file-previews (:config @system) conn store file-spec)

        (let [file-metadata-after-preview (first (metadata-store/get-normalized-metadata! [filename] conn))
              generated-previews          (vec (metadata-store/get-previews filename conn))]
          (assert-preview-generation-errored file-metadata-after-preview generated-previews))))))

(deftest previews-are-deleted-when-file-is-deleted
  (let [store (u/new-in-memory-store)
        conn  {:connection (:db @system)}]
    (doseq [{:keys [filename file-object content-type size]} (take 2 fixtures/ok-files)]
      (let [uploaded  (-> (t/now)
                          (.getMillis)
                          (Timestamp.))
            origin-system "Test-system"
            origin-reference "1.2.246.562.11.000000000000000000001"
            file-spec {:key             filename
                       :filename        filename
                       :content-type    content-type
                       :size            size
                       :uploaded        uploaded
                       :origin-system    origin-system
                       :origin-reference origin-reference}]
        (test-metadata-store/create-file file-spec conn)
        (file-store/create-file store file-object filename)
        (metadata-store/set-virus-scan-status! filename "done" conn)
        (metadata-store/finalize-files [filename] origin-system origin-reference conn)
        (preview-generator/generate-file-previews (:config @system) conn store file-spec)

        (file-store/delete-file-and-metadata (:key file-spec) "preview-generator-test" store conn false)

        (let [file-metadata-after-preview (first (metadata-store/get-normalized-metadata! [filename] conn))
              generated-previews          (vec (metadata-store/get-previews filename conn))]
          (is (= 0 (count generated-previews)))
          (is (= nil file-metadata-after-preview)))))))
