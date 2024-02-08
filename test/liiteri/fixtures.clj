(ns liiteri.fixtures
  (:require [clojure.java.io :as io]))

(defn load-test-file [{:keys [filename content-type]}]
  (let [file-object (io/file (io/resource (format "test-files/%s" filename)))]
    {:filename     filename
     :file-object  file-object
     :content-type content-type
     :size         (.length file-object)}))

(defn load-mangled-extension-test-file [{:keys [mangled-filename filename content-type]}]
  (assoc (load-test-file {:filename filename :content-type content-type}) :mangled-filename mangled-filename))

(def file-types
  {:exe  "application/octet-stream"
   :txt  "text/plain"
   :jpg  "image/jpeg"
   :png  "image/png"
   :gif  "image/gif"
   :heic  "image/heic"
   :rtf  "application/rtf"
   :pdf  "application/pdf"
   :odt  "application/vnd.oasis.opendocument.text"
   :doc  "application/msword"
   :docx "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
   :ods  "application/vnd.oasis.opendocument.spreadsheet"
   :xls  "application/vnd.ms-excel"
   :xlsx "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
   :mp4 "video/mp4"})

(def forbidden-files [(load-test-file {:filename "sample.exe" :content-type (:exe file-types)})])

(def ok-files [(load-test-file {:filename "sample.doc" :content-type (:doc file-types)})
               (load-test-file {:filename "sample.docx" :content-type (:docx file-types)})
               (load-test-file {:filename "sample.heic" :content-type (:heic file-types)})
               (load-test-file {:filename "sample.jpg" :content-type (:jpg file-types)})
               (load-test-file {:filename "sample.ods" :content-type (:ods file-types)})
               (load-test-file {:filename "sample.odt" :content-type (:odt file-types)})
               (load-test-file {:filename "sample.pdf" :content-type (:pdf file-types)})
               (load-test-file {:filename "sample.png" :content-type (:png file-types)})
               (load-test-file {:filename "sample.txt" :content-type (:txt file-types)})
               (load-test-file {:filename "sample.xls" :content-type (:xls file-types)})
               (load-test-file {:filename "sample.xlsx" :content-type (:xlsx file-types)})
               (load-test-file {:filename "sample.mp4" :content-type (:mp4 file-types)})])

(def mangled-extension-files [(load-mangled-extension-test-file {:mangled-filename "sample.docx" :filename "sample.doc" :content-type (:doc file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.doc" :filename "sample.docx" :content-type (:docx file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.png" :filename "sample.jpg" :content-type (:jpg file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.qt" :filename "sample.heic" :content-type (:heic file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.xls" :filename "sample.ods" :content-type (:ods file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.doc" :filename "sample.odt" :content-type (:odt file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.doc" :filename "sample.pdf" :content-type (:pdf file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.jpg" :filename "sample.png" :content-type (:png file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.doc" :filename "sample.txt" :content-type (:txt file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.odt" :filename "sample.xls" :content-type (:xls file-types)})
                              (load-mangled-extension-test-file {:mangled-filename "sample.xls" :filename "sample.xlsx" :content-type (:xlsx file-types)})])
