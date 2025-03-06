(ns liiteri.file-store-test
  (:require [clj-time.core :as t]
            [clojure.string :as string]
            [clojure.test :refer :all]
            [liiteri.core :as system]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.files.file-store :as file-store]
            [liiteri.test-utils :as u])
  (:import [java.util UUID]
           [java.sql Timestamp]))

(def system (atom (system/new-system false)))
(def metadata1 (atom nil))
(def metadata2 (atom nil))
(def metadata3 (atom nil))
(def metadata4 (atom nil))
(def metadata5 (atom nil))

(def application-key1 "1.2.246.562.11.000000000000000000001")
(def application-key2 "1.2.246.562.11.000000000000000000002")
(def application-key3 "1.2.246.562.11.000000000000000000003")
(def application-key4 "1.2.246.562.11.000000000000000000004")
(def application-key5 "1.2.246.562.11.000000000000000000005")
(use-fixtures :once
              (fn [tests]
                (u/start-system system)
                (tests)
                (u/clear-database! system)
                (u/stop-system system)))

(defn- init-test-files []
  (let [filename1 "test-file.txt"
        filename2 "test-file-2.txt"
        filename3 "test-file-3.txt"
        filename4 "test-file-4.txt"
        filename5 "test-file-5.txt"
        file-key1 (str (UUID/randomUUID))
        file-key2 (str (UUID/randomUUID))
        file-key3 (str (UUID/randomUUID))
        file-key4 (str (UUID/randomUUID))
        file-key5 (str (UUID/randomUUID))
        origin-system "Test-system"
        conn {:connection (:db @system)}
        store (:storage-engine @system)
        uploaded (-> (t/now)
                     (t/minus (t/months 1))
                     (.getMillis)
                     (Timestamp.))
        deleted (-> (t/now)
                    (t/minus (t/days 1))
                    (.getMillis)
                    (Timestamp.))]
    (file-store/create-file-from-bytearray store (.getBytes "test file1") file-key1)
    (file-store/create-file-from-bytearray store (.getBytes "test file2") file-key2)
    (file-store/create-file-from-bytearray store (.getBytes "test file3") file-key3)
    (file-store/create-file-from-bytearray store (.getBytes "test file4") file-key4)
    (file-store/create-file-from-bytearray store (.getBytes "test file5") file-key5)
    (reset! metadata1 (test-metadata-store/create-file {:key              file-key1
                                                        :filename         filename1
                                                        :content-type     "text/plain"
                                                        :size             1
                                                        :uploaded         uploaded
                                                        :origin-system    origin-system
                                                        :origin-reference application-key1}
                                                       conn))
    (reset! metadata2 (test-metadata-store/create-file {:key              file-key2
                                                        :filename         filename2
                                                        :content-type     "text/plain"
                                                        :size             1
                                                        :uploaded         uploaded
                                                        :origin-system    origin-system
                                                        :origin-reference application-key2}
                                                       conn))
    (reset! metadata3 (test-metadata-store/create-file {:key              file-key3
                                                        :filename         filename3
                                                        :content-type     "text/plain"
                                                        :size             1
                                                        :uploaded         uploaded
                                                        :origin-system    origin-system
                                                        :origin-reference application-key3}
                                                       conn))
    (reset! metadata4 (test-metadata-store/create-file {:key              file-key4
                                                        :filename         filename4
                                                        :content-type     "text/plain"
                                                        :size             1
                                                        :uploaded         uploaded
                                                        :origin-system    origin-system
                                                        :origin-reference application-key4}
                                                       conn))
    (reset! metadata5 (test-metadata-store/create-file {:key              file-key5
                                                        :filename         filename5
                                                        :content-type     "text/plain"
                                                        :size             1
                                                        :uploaded         uploaded
                                                        :deleted          deleted
                                                        :origin-system    origin-system
                                                        :origin-reference application-key5}
                                                       conn))))

(defn- remove-test-files []
  (metadata-store/delete-file (:key @metadata1) "file-store-test" {:connection (:db @system)} false)
  (metadata-store/delete-file (:key @metadata2) "file-store-test" {:connection (:db @system)} false)
  (metadata-store/delete-file (:key @metadata3) "file-store-test" {:connection (:db @system)} false)
  (metadata-store/delete-file (:key @metadata4) "file-store-test" {:connection (:db @system)} false)
  (metadata-store/delete-file (:key @metadata5) "file-store-test" {:connection (:db @system)} false)
  (file-store/delete-file (:storage-engine @system) (:key @metadata1))
  (file-store/delete-file (:storage-engine @system) (:key @metadata2))
  (file-store/delete-file (:storage-engine @system) (:key @metadata3))
  (file-store/delete-file (:storage-engine @system) (:key @metadata4))
  (file-store/delete-file (:storage-engine @system) (:key @metadata5)))

(use-fixtures :each
              (fn [tests]
                (tests)
                (remove-test-files)))

(deftest files-and-metadata-removed-by-application-key
  (let [db (:db @system)
        storage-engine (:storage-engine @system)]
    (init-test-files)

    (file-store/delete-files-and-metadata-by-origin-references [application-key2
                                                                application-key3
                                                                application-key4
                                                                application-key5
                                                                "1.2.246.562.11.000000000000000000009"]
                                                               "file-store-test"
                                                               storage-engine
                                                               {:connection db}
                                                               {})

    (let [metadata1 (test-metadata-store/get-metadata-for-tests [(:key @metadata1)] {:connection db})
          metadata2 (test-metadata-store/get-metadata-for-tests [(:key @metadata2)] {:connection db})
          metadata3 (test-metadata-store/get-metadata-for-tests [(:key @metadata3)] {:connection db})
          metadata4 (test-metadata-store/get-metadata-for-tests [(:key @metadata4)] {:connection db})
          metadata5 (test-metadata-store/get-metadata-for-tests [(:key @metadata5)] {:connection db})]

      (is (nil? (:deleted metadata1)))
      (is (some? (:deleted metadata2)))
      (is (some? (:deleted metadata3)))
      (is (some? (:deleted metadata4)))
      (is (some? (:deleted metadata5)))
      (is (file-store/file-exists? (:storage-engine @system) (:key metadata1)))
      (is (not (file-store/file-exists? (:storage-engine @system) (:key metadata2))))
      (is (not (file-store/file-exists? (:storage-engine @system) (:key metadata3))))
      (is (not (file-store/file-exists? (:storage-engine @system) (:key metadata4))))
      (is (not (file-store/file-exists? (:storage-engine @system) (:key metadata5)))))))

(deftest not-to-be-deleted-filekeys-preserved-when-deleting-file-and-metadata
  (init-test-files)
  (let [db (:db @system)
        storage-engine (:storage-engine @system)
        filekey1 (:key @metadata1)
        filekey2 (:key @metadata2)
        filekey3 (:key @metadata3)
        config {:filekeys-not-to-be-deleted (string/join #"," [filekey1 filekey2])}

        counts1 (file-store/delete-file-and-metadata filekey1 "1.2.246.562.24.000000000000000000001" storage-engine
                                                     {:connection db} config false)
        counts2 (file-store/delete-file-and-metadata filekey2 "1.2.246.562.24.000000000000000000001" storage-engine
                                                     {:connection db} config false)
        counts3 (file-store/delete-file-and-metadata filekey3 "1.2.246.562.24.000000000000000000001" storage-engine
                                                     {:connection db} config false)]

    (is {:ignored 1, :deleted 0} counts1)
    (is {:ignored 1, :deleted 0} counts2)
    (is {:ignored 0, :deleted 1} counts3)
    (is (nil? (:deleted (test-metadata-store/get-metadata-for-tests [filekey1] {:connection db}))))
    (is (nil? (:deleted (test-metadata-store/get-metadata-for-tests [filekey2] {:connection db}))))
    (is (some? (:deleted (test-metadata-store/get-metadata-for-tests [filekey3] {:connection db}))))
    (is (file-store/file-exists? (:storage-engine @system) filekey1))
    (is (file-store/file-exists? (:storage-engine @system) filekey2))
    (is (not (file-store/file-exists? (:storage-engine @system) filekey3)))))

(deftest not-to-be-deleted-filekeys-preserved-when-removing-by-application-key
  (init-test-files)
  (let [db (:db @system)
        storage-engine (:storage-engine @system)
        filekey1 (:key @metadata1)
        filekey2 (:key @metadata2)
        filekey3 (:key @metadata3)
        filekey4 (:key @metadata4)
        filekey5 (:key @metadata5)

        keys (file-store/delete-files-and-metadata-by-origin-references [application-key1
                                                                         application-key2
                                                                         application-key3
                                                                         application-key4
                                                                         application-key5
                                                                         "1.2.246.562.11.000000000000000000009"]
                                                                        "file-store-test"
                                                                        storage-engine
                                                                        {:connection db}
                                                                        {:filekeys-not-to-be-deleted
                                                                         (string/join #"," [filekey1 filekey2])})]

    (is [filekey1 filekey2] (:ignored-keys keys))
    (is [filekey3 filekey4 filekey5] (:deleted-keys keys))

    (is (nil? (:deleted (test-metadata-store/get-metadata-for-tests [filekey1] {:connection db}))))
    (is (nil? (:deleted (test-metadata-store/get-metadata-for-tests [filekey2] {:connection db}))))
    (is (some? (:deleted (test-metadata-store/get-metadata-for-tests [filekey3] {:connection db}))))
    (is (some? (:deleted (test-metadata-store/get-metadata-for-tests [filekey4] {:connection db}))))
    (is (some? (:deleted (test-metadata-store/get-metadata-for-tests [filekey5] {:connection db}))))
    (is (file-store/file-exists? (:storage-engine @system) filekey1))
    (is (file-store/file-exists? (:storage-engine @system) filekey2))
    (is (not (file-store/file-exists? (:storage-engine @system) filekey3)))
    (is (not (file-store/file-exists? (:storage-engine @system) filekey4)))
    (is (not (file-store/file-exists? (:storage-engine @system) filekey5)))))
