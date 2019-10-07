(ns liiteri.pdf-to-png-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pantomime.mime :as mime]
            [liiteri.pdf-to-png :as pdf-to-png]))

(defn file->bytes [file]
  (with-open [xin (io/input-stream file)
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(deftest pdf-is-converted
  (let [pdf-file (io/file (io/resource "three_page_pdf_for_testing.pdf"))
        pdf-bytes (file->bytes pdf-file)
        png-files (pdf-to-png/pdf->pngs pdf-bytes)]
    (is (= 3 (count png-files)))
    (is (every? #(= "image/png" (mime/mime-type-of %)) png-files))))
