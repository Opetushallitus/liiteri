(ns liiteri.mime-test
  (:require [liiteri.mime :as mime]
            [liiteri.config :as config]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            ))

(def config (config/new-config))

(deftest mime-type-detected
  (let [parrot-file (io/file (io/resource "parrot.png"))]
    (mime/validate-file-content-type! config parrot-file "parrot.png" "application/pngf")))

(deftest extensions-fixed
  )
