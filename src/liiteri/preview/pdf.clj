(ns liiteri.preview.pdf
  (:require [liiteri.preview.util :as util]
            [liiteri.preview.interface :as interface]
            [liiteri.config :as config])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (org.apache.pdfbox.rendering PDFRenderer ImageType)))

(def dpi 300)

(defn- bytes->pdf-document [^bytes pdfbytes] (PDDocument/load pdfbytes))
(defn- get-page-count [pdf-document] (.getNumberOfPages pdf-document))

(defn pdf->pngs [pdf-document start-index max-page-count page-count]
  (let [pdf-renderer (PDFRenderer. pdf-document)
        page-range (range 0 (min max-page-count
                                 page-count))
        buffered-images (map #(.renderImageWithDPI pdf-renderer % dpi ImageType/RGB) page-range)]
    (mapv util/buffered-image->bytes buffered-images)))

(defmethod interface/generate-previews-for-file "application/pdf" [conn storage-engine file input-stream max-page-count]
  (let [pdf-document (-> (util/inputstream->bytes input-stream)
                         bytes->pdf-document)
        page-count (get-page-count pdf-document)
        pdfs (pdf->pngs pdf-document 0 max-page-count page-count)]
    (.close pdf-document)
    [page-count pdfs]))
