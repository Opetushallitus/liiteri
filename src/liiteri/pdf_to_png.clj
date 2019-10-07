(ns liiteri.pdf-to-png
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

(defn pdf->pngs [^bytes pdfbytes]
  (let [pd-document (PDDocument/load pdfbytes)
        pdf-renderer (PDFRenderer. pd-document)
        page-range (range 0 (.getNumberOfPages pd-document))
        buffered-images (map #(.renderImageWithDPI pdf-renderer % dpi ImageType/RGB) page-range)]
    (map buffered-image->bytes buffered-images)))
