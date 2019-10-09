(ns liiteri.mime-test
  (:require [liiteri.mime :as mime]
            [liiteri.config :as config]
            [liiteri.fixtures :refer :all]
            [clojure.test :refer :all]
            [taoensso.timbre :as log]))

(def config (config/new-config))

(deftest ok-mime-type-allowed
  (doseq [[name _ content-type] ok-files]
    (do
      (log/info (format "Testing %s with content-type %s (should pass)" name content-type))
      (mime/validate-file-content-type! config name content-type content-type))))

(deftest forbidden-mime-type-rejected
  (doseq [[name _ content-type] forbidden-files]
    (do
      (log/info (format "Testing %s with content-type %s (should throw exception)" name content-type))
      (is (thrown? clojure.lang.ExceptionInfo (mime/validate-file-content-type! config name content-type content-type))))))

(deftest extensions-fixed
  )
