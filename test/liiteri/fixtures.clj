(ns liiteri.fixtures
  (:require [clojure.java.io :as io]))

(defn load-test-file [filename content-type]
  (let [file-object (io/file (io/resource (format "test-files/%s" filename)))]
    [filename file-object content-type (.length file-object)]))

(def file-types
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

(def forbidden-files [(load-test-file "sample.exe" (:exe file-types))])

(def ok-files [(load-test-file "sample.doc" (:doc file-types))
               (load-test-file "sample.docx" (:docx file-types))
               (load-test-file "sample.jpg" (:jpg file-types))
               (load-test-file "sample.ods" (:ods file-types))
               (load-test-file "sample.odt" (:odt file-types))
               (load-test-file "sample.pdf" (:pdf file-types))
               (load-test-file "sample.png" (:png file-types))
               (load-test-file "sample.txt" (:txt file-types))
               (load-test-file "sample.xls" (:xls file-types))
               (load-test-file "sample.xlsx" (:xlsx file-types))])
