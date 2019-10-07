(ns liiteri.mime-test
  (:require [liiteri.mime :as mime]
            [liiteri.config :as config]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            ))

(def config (config/new-config))

(deftest mime-type-detected
  (let [png-file (io/file (io/resource "test-files/sample.png"))]
    (mime/validate-file-content-type! config png-file "sample.png" "image/png" "image/png")
    ))

(deftest extensions-fixed
  )
