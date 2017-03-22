(ns liiteri.files.file-store)

(defprotocol FileStore
  (create-file [this file])

  (update-file [this file file-key])

  (delete-file [this file-key])

  (get-file [this file-key]))
