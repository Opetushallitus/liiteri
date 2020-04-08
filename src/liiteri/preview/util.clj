(ns liiteri.preview.util
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream)
           (javax.imageio ImageIO)
           (java.awt.image BufferedImage)))

(defn buffered-image->bytes [^BufferedImage image]
  (with-open [output-stream (ByteArrayOutputStream.)]
    (ImageIO/write image "png" output-stream)
    (.toByteArray output-stream)))

(defn inputstream->bytes [inputstream]
  (with-open [xin  inputstream
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))
