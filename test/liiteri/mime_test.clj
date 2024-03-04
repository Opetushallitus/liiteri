(ns liiteri.mime-test
  (:require [liiteri.mime :as mime]
            [liiteri.config :as config]
            [liiteri.fixtures :refer :all]
            [clojure.test :refer :all]
            [taoensso.timbre :as log])
  (:import [java.io FileInputStream]))

(def config (config/new-config))

(deftest ok-mime-type-allowed
  (doseq [{:keys [filename content-type]} ok-files]
    (do
      (log/info (format "Testing %s with content-type %s (should pass)" filename content-type))
      (mime/validate-file-content-type! config filename content-type))))

(deftest forbidden-mime-type-rejected
  (doseq [{:keys [filename content-type]} forbidden-files]
    (do
      (log/info (format "Testing %s with content-type %s (should throw exception)" filename content-type))
      (is (thrown? clojure.lang.ExceptionInfo (mime/validate-file-content-type! config filename content-type))))))

(deftest ok-mime-type-recognized
  (doseq [{:keys [filename content-type file-object]} ok-files]
    (do
      (log/info (format "Testing %s should be recognized as %s" filename content-type))
      (is (= (:content-type (mime/file->validated-file-spec! config filename #(identity {:size 0 :file (FileInputStream. file-object)}))) content-type)))))

(deftest extensions-fixed
  (is (= "foobar.png" (mime/fix-extension "foobar" "image/png")))
  (is (= "foobar.png" (mime/fix-extension "foobar." "image/png")))
  (is (= "foobar.png" (mime/fix-extension "foobar.jpg" "image/png")))
  (is (= "foobar" (mime/fix-extension "foobar" nil))))
