(ns liiteri.mime
  (:require [taoensso.timbre :as log]
            [ring.util.http-response :as http-response]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io])
  (:import (org.apache.tika Tika)
           (org.apache.tika.config TikaConfig)
           (org.apache.tika.mime MimeType MimeTypes MimeTypeException)
           (java.io InputStream)
           (org.apache.commons.io.input BoundedInputStream)
           (org.apache.tika.io TikaInputStream)
           (org.apache.tika.parser AutoDetectParser)
           (org.apache.tika.metadata Metadata)
           (org.apache.tika.parser ParseContext)
           (org.apache.tika.sax BodyContentHandler)
           (com.amazonaws.services.s3.model S3ObjectInputStream)))

(def ^TikaConfig tikaConfig (TikaConfig. (io/resource "tika-config.xml"))) ;; config needed for excluding some parsers
(def ^Tika detector (Tika. tikaConfig))
(def ^MimeTypes mimetypes (MimeTypes/getDefaultMimeTypes))
(def ^AutoDetectParser parser (AutoDetectParser. tikaConfig))

(defn detect-mime-type [get-stream]
  (with-open [^InputStream inputstream (get-stream)
              ^TikaInputStream tika-stream (TikaInputStream/get inputstream)]
    (let [mimetype (.detect detector tika-stream)]
      (when (instance? S3ObjectInputStream inputstream) (.abort inputstream))
      mimetype)))

(defn parse-mime-type [get-stream]
  (with-open [^InputStream inputstream (get-stream)
              ; luetaan vain ensimmäiset 500 kilotavua, muuten isojen tiedostojen parserointi räjähtää
              ^BoundedInputStream bounded-inputstream (BoundedInputStream. inputstream (* 512 1024))
              ^TikaInputStream tika-stream (TikaInputStream/get bounded-inputstream)]
    (let [handler (BodyContentHandler.) metadata (Metadata.) context (ParseContext.)]
      (.parse parser tika-stream handler metadata context)
      (when (instance? S3ObjectInputStream inputstream) (.abort inputstream))
      (.get metadata "Content-Type"))))

(defn validate-file-content-type! [config filename real-content-type]
  (let [allowed-mime-types (-> config :file-store :attachment-mime-types)]
    (if (not-any? (partial = real-content-type) allowed-mime-types)
      (log/warn (str "Request with illegal content-type '" real-content-type "' of file '" filename ". Allowed: " allowed-mime-types)
                (http-response/bad-request! {:illegal-content-type  real-content-type
                                             :allowed-content-types allowed-mime-types}))
      real-content-type)))

(defn fix-extension [filename real-content-type-name]
  (try
    (let [^MimeType mimetype-by-name (.forName mimetypes real-content-type-name)
          extension-from-mimetype    (if-let [mt mimetype-by-name]
                                       (.getExtension mt)
                                       "")
          [fname]                    (fs/split-ext filename)]
      (format "%s%s" fname extension-from-mimetype))
    (catch MimeTypeException e
      (log/warn
       (str "Could not get extension for content-type '" real-content-type-name "', exception: " (.getMessage e)))
      (first (fs/split-ext filename)))))

(defn file->validated-file-spec! [config filename get-file]
  (let [{:keys [size file]} (get-file)
        detected-content-type (detect-mime-type #(identity file))
        parsed-content-type (if (= detected-content-type "video/quicktime") (or (parse-mime-type #(:file (get-file))) detected-content-type) detected-content-type)
        updated-filename      (fix-extension filename parsed-content-type)]
      (validate-file-content-type! config updated-filename parsed-content-type)
      {:content-type parsed-content-type
       :filename     updated-filename
       :size         size}))
