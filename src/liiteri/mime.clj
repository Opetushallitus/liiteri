(ns liiteri.mime
  (:require [taoensso.timbre :as log]
            [ring.util.http-response :as http-response]
            [me.raynes.fs :as fs])
  (:import (org.apache.tika Tika)
           (org.apache.tika.mime MimeType MimeTypes MimeTypeException)))

(def ^Tika detector (Tika.))
(def ^MimeTypes mimetypes (MimeTypes/getDefaultMimeTypes))

(defn detect-mime-type [file-or-stream-or-buffer]
  (.detect detector file-or-stream-or-buffer))

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

(defn file->validated-file-spec! [config filename tempfile size]
  (let [detected-content-type (detect-mime-type tempfile)
        updated-filename      (fix-extension filename detected-content-type)]
    (validate-file-content-type! config updated-filename detected-content-type)
    {:content-type detected-content-type
     :filename     updated-filename
     :size         size
     :tempfile     tempfile}))
