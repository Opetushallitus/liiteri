(ns liiteri.preview.pdf-to-png
  (:require [clojure.java.io :as io])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (org.apache.pdfbox.rendering PDFRenderer ImageType)
           (java.io ByteArrayOutputStream)
           (javax.imageio ImageIO)
           (java.awt.image BufferedImage)))

(def dpi 300)

(defn- buffered-image->bytes [^BufferedImage image]
  (with-open [output-stream (ByteArrayOutputStream.)]
    (ImageIO/write image "png" output-stream)
    (.toByteArray output-stream)))

(defn inputstream->bytes [inputstream]
  (with-open [xin inputstream
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

(defn pdf->pngs [^bytes pdfbytes]
  (let [pd-document (PDDocument/load pdfbytes)
        pdf-renderer (PDFRenderer. pd-document)
        page-range (range 0 (.getNumberOfPages pd-document))
        buffered-images (map #(.renderImageWithDPI pdf-renderer % dpi ImageType/RGB) page-range)]
    (map buffered-image->bytes buffered-images)))

(defn inputstream->pngs [inputstream]
  (-> (inputstream->bytes inputstream)
      pdf->pngs))
