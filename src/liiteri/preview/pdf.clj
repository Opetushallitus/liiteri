(ns liiteri.preview.pdf
  (:require [liiteri.preview.util :as util]
            [liiteri.preview.interface :as interface])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (org.apache.pdfbox.rendering PDFRenderer ImageType)))

(defn- bytes->pdf-document [^bytes pdfbytes] (PDDocument/load pdfbytes))
(defn- get-page-count [pdf-document] (.getNumberOfPages pdf-document))

(defn pdf->pngs [pdf-document start-index max-page-count page-count dpi]
  (let [pdf-renderer (PDFRenderer. pdf-document)
        page-range (range 0 (min max-page-count
                                 page-count))
        buffered-images (map #(.renderImageWithDPI pdf-renderer % dpi ImageType/RGB) page-range)]
    (mapv util/buffered-image->bytes buffered-images)))

(defmethod interface/generate-previews-for-file "application/pdf" [storage-engine file input-stream config]
  (let [pdf-document (-> (util/inputstream->bytes input-stream)
                         bytes->pdf-document)
        page-count (get-page-count pdf-document)
        max-page-count (get-in config [:preview-generator :preview-page-count])
        dpi (get-in config [:preview-generator :pdf :dpi])
        pdfs (pdf->pngs pdf-document 0 max-page-count page-count dpi)]
    (.close pdf-document)
    [page-count pdfs]))

(def content-types ["application/pdf"])
