(ns liiteri.virus-scan-test
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [liiteri.core :as system]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.db.test-metadata-store :as test-metadata-store]
            [liiteri.test-utils :as u]
            [liiteri.virus-scan :as virus-scan]
            [org.httpkit.client :as http]
            [ring.util.http-response :as response])
  (:import [java.io File]
           [java.util UUID]))

(def system (atom nil))
(def metadata (atom nil))
(def file (atom nil))

(defn- init-test-file []
  (jdbc/with-db-transaction [datasource (:db @system)]
    (let [filename "test-file.txt"
          file-key (str (UUID/randomUUID))
          conn     {:connection datasource}
          base-dir (get-in (:config @system) [:file-store :filesystem :base-path])]
      (with-open [w (io/writer (str base-dir "/" file-key))]
        (.write w "test file\n"))
      (reset! metadata (metadata-store/create-file {:key          file-key
                                                    :filename     filename
                                                    :content-type "text/plain"
                                                    :size         1}
                                                   conn))
      (reset! file (io/file (str base-dir "/" file-key))))))

(use-fixtures :once
  (fn [tests]
    (u/start-system system)
    (u/create-temp-dir system)
    (init-test-file)
    (tests)
    (u/clear-database! system)
    (u/stop-system system)
    (u/remove-temp-dir system)))

(deftest virus-scan-for-clean-file
  (let [db             (:db @system)
        storage-engine (:storage-engine @system)
        config         (:config @system)]
    (with-redefs [http/post (fn [& _]
                              (future (response/ok "Everything ok : true\n")))]
      (#'virus-scan/scan-files db storage-engine config))
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] db)]
      (is (= (:virus-scan-status metadata) "done")))))
