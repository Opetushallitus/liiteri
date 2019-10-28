(ns liiteri.mime-fixer-test
  (:require [liiteri.mime-fixer :as mime-fixer]
            [liiteri.core :as system]
            [liiteri.files.file-store :as file-store]
            [liiteri.db.test-metadata-store :as metadata-store]
            [liiteri.fixtures :refer :all]
            [liiteri.test-utils :as u]
            [com.stuartsierra.component :as component]
            [clj-time.core :as t]
            [clojure.test :refer :all])
  (:import [java.sql Timestamp]))

(def system (atom (system/new-system {})))

(use-fixtures :once
  (fn [tests]
    (u/start-system system)
    (u/create-temp-dir system)
    (component/stop (:mime-fixer @system))
    (tests)
    (u/clear-database! system)
    (u/stop-system system)
    (u/remove-temp-dir system)))

(deftest mime-fixer-ok-files
  (let [store (u/new-in-memory-store)
        conn {:connection (:db @system)}]
    (doseq [[mangled-filename filename file content-type size] mangled-extension-files]
      (let [uploaded (-> (t/now)
                         (.getMillis)
                         (Timestamp.))
            file-spec {:key          filename
                       :filename     mangled-filename
                       :content-type nil
                       :size         size
                       :uploaded     uploaded}]
        (metadata-store/create-file file-spec conn)
        (file-store/create-file store file filename)
        (mime-fixer/fix-mime-type-of-file conn store {:key filename
                                                      :filename mangled-filename
                                                      :uploaded uploaded})
        (let [fixed-metadata (metadata-store/get-metadata-for-tests [filename] conn)]
          (is (= content-type (:content-type fixed-metadata)))
          (is (= filename (:filename fixed-metadata))))))))
