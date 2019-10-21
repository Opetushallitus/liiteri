(ns liiteri.preview.preview-generator-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [liiteri.preview.preview-generator :as preview-generator]))

(deftest pdf-is-converted
  (let [pdf-file (io/file (io/resource "three_page_pdf_for_testing.pdf"))
        pdf-bytes (file->bytes pdf-file)
        png-files (pdf-to-png/pdf->pngs pdf-bytes)]
    (is (= 3 (count png-files)))
    (is (every? #(= "image/png" (mime/mime-type-of %)) png-files))))
