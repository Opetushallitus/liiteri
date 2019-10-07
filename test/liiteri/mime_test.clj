(ns liiteri.mime-test
  (:require [liiteri.mime :as mime]
            [liiteri.config :as config]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [taoensso.timbre :as log]
            ))

(def config (config/new-config))

(defn load-test-file [filename content-type]
  (let [file-object (io/file (io/resource (format "test-files/%s" filename)))]
    [filename file-object content-type]))

(def types
  {:exe "application/octet-stream"
   :txt "text/plain"
   :jpg "image/jpeg"
   :png "image/png"
   :gif "image/gif"
   :rtf "application/rtf"
   :pdf "application/pdf"
   :odt "application/vnd.oasis.opendocument.text"
   :doc "application/msword"
   :docx "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
   :ods "application/vnd.oasis.opendocument.spreadsheet"
   :xls "application/vnd.ms-excel"
   :xlsx "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})

(def forbidden-files [(load-test-file "sample.exe" (:exe types))])

(def ok-files [(load-test-file "sample.doc" (:doc types))
               (load-test-file "sample.docx" (:docx types))
               (load-test-file "sample.jpg" (:jpg types))
               (load-test-file "sample.ods" (:ods types))
               (load-test-file "sample.odt" (:odt types))
               (load-test-file "sample.pdf" (:pdf types))
               (load-test-file "sample.png" (:png types))
               (load-test-file "sample.txt" (:txt types))
               (load-test-file "sample.xls" (:xls types))
               (load-test-file "sample.xlsx" (:xlsx types))])

(deftest ok-mime-type-allowed
  (doseq [[name file content-type] ok-files]
    (do
      (log/info (format "Testing %s with content-type %s (should pass)" name content-type))
      (mime/validate-file-content-type! config file name content-type content-type))))

(deftest forbidden-mime-type-rejected
  (doseq [[name file content-type] forbidden-files]
    (do
      (log/info (format "Testing %s with content-type %s (should throw exception)" name content-type))
      (is (thrown? clojure.lang.ExceptionInfo (mime/validate-file-content-type! config file name content-type content-type))))))

(deftest extensions-fixed
  )
