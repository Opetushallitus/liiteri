(ns liiteri.file-store-test
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [liiteri.core :as system]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.files.file-store :as file-store]
            [liiteri.file-cleaner :as cleaner]
            [liiteri.test-utils :as u]
            [taoensso.timbre :as log])
  (:import [java.util UUID]
           [java.sql Timestamp]
           [java.io File]))

(def system (atom (system/new-system)))
(def metadata1 (atom nil))
(def metadata2 (atom nil))
(def metadata3 (atom nil))
(def metadata4 (atom nil))
(def file1 (atom nil))
(def file2 (atom nil))
(def file3 (atom nil))
(def file4 (atom nil))

(use-fixtures :once
              (fn [tests]
                (u/start-system system)
                (u/create-temp-dir system)
                (tests)
                (u/clear-database! system)
                (u/stop-system system)
                (u/remove-temp-dir system)))

(defn- init-test-files [uploaded]
  (let [filename1 "test-file.txt"
        filename2 "test-file-2.txt"
        filename3 "test-file-3.txt"
        filename4 "test-file-4.txt"
        file-key1 (str (UUID/randomUUID))
        file-key2 (str (UUID/randomUUID))
        file-key3 (str (UUID/randomUUID))
        file-key4 (str (UUID/randomUUID))
        application-key1 "1.2.246.562.11.000000000000000000001"
        application-key2 "1.2.246.562.11.000000000000000000002"
        application-key3 "1.2.246.562.11.000000000000000000003"
        application-key4 "1.2.246.562.11.000000000000000000003"
        conn {:connection (:db @system)}
        base-dir (get-in (:config @system) [:file-store :filesystem :base-path])]
    (with-open [w (io/writer (str base-dir "/" file-key1))]
      (.write w "test file\n"))
    (with-open [w (io/writer (str base-dir "/" file-key2))]
      (.write w "test file2\n"))
    (with-open [w (io/writer (str base-dir "/" file-key3))]
      (.write w "test file3\n"))
    (with-open [w (io/writer (str base-dir "/" file-key4))]
      (.write w "test file4\n"))
    (reset! metadata1 (test-metadata-store/create-file {:key             file-key1
                                                        :filename        filename1
                                                        :content-type    "text/plain"
                                                        :size            1
                                                        :uploaded        uploaded
                                                        :application-key application-key1}
                                                       conn))
    (reset! metadata2 (test-metadata-store/create-file {:key             file-key2
                                                        :filename        filename2
                                                        :content-type    "text/plain"
                                                        :size            1
                                                        :uploaded        uploaded
                                                        :application-key application-key2}
                                                       conn))
    (reset! metadata3 (test-metadata-store/create-file {:key             file-key3
                                                        :filename        filename3
                                                        :content-type    "text/plain"
                                                        :size            1
                                                        :uploaded        uploaded
                                                        :application-key application-key3}
                                                       conn))
    (reset! metadata4 (test-metadata-store/create-file {:key             file-key4
                                                        :filename        filename4
                                                        :content-type    "text/plain"
                                                        :size            1
                                                        :uploaded        uploaded
                                                        :application-key application-key4}
                                                       conn))
    (reset! file1 (io/file (str base-dir "/" file-key1)))
    (reset! file2 (io/file (str base-dir "/" file-key2)))
    (reset! file3 (io/file (str base-dir "/" file-key3)))
    (reset! file4 (io/file (str base-dir "/" file-key4)))))

(defn- remove-test-files []
  (metadata-store/delete-file (:key @metadata1) {:connection (:db @system)})
  (metadata-store/delete-file (:key @metadata2) {:connection (:db @system)})
  (metadata-store/delete-file (:key @metadata3) {:connection (:db @system)})
  (metadata-store/delete-file (:key @metadata4) {:connection (:db @system)})
  (io/delete-file @file1 true)
  (io/delete-file @file2 true)
  (io/delete-file @file3 true)
  (io/delete-file @file4 true)

  )

(use-fixtures :each
              (fn [tests]
                (tests)
                (remove-test-files)))

(deftest files-and-metadata-removed-by-application-key
  (let [db (:db @system)
        storage-engine (:storage-engine @system)
        config (:config @system)
        uploaded (-> (t/now)
                     (t/minus (t/months 1))
                     (.getMillis)
                     (Timestamp.))]
    (init-test-files uploaded)

    (file-store/delete-files-and-metadata-by-application-keys ["1.2.246.562.11.000000000000000000002"
                                                               "1.2.246.562.11.000000000000000000003"
                                                               "1.2.246.562.11.000000000000000000009"]
                                                              storage-engine
                                                              {:connection db})

    (let [metadata1 (test-metadata-store/get-metadata-for-tests [(:key @metadata1)] {:connection db})
          metadata2 (test-metadata-store/get-metadata-for-tests [(:key @metadata2)] {:connection db})
          metadata3 (test-metadata-store/get-metadata-for-tests [(:key @metadata3)] {:connection db})
          metadata4 (test-metadata-store/get-metadata-for-tests [(:key @metadata4)] {:connection db})]

      (is (nil? (:deleted metadata1)))
      (is (some? (:deleted metadata2)))
      (is (some? (:deleted metadata3)))
      (is (some? (:deleted metadata4)))
      (is (.exists (File. (.getPath @file1))))
      (is (not (.exists (File. (.getPath @file2)))))
      (is (not (.exists (File. (.getPath @file3)))))
      (is (not (.exists (File. (.getPath @file4))))))))
