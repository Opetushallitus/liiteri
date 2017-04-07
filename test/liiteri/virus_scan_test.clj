(ns liiteri.virus-scan-test
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [liiteri.core :as system]
            [liiteri.db.file-metadata-store :as metadata-store]
            [liiteri.test-metadata-store :as test-metadata-store]
            [liiteri.virus-scan :as virus-scan])
  (:import [java.io File]
           [java.util UUID]))

(def system-state (atom nil))
(def metadata (atom nil))
(def file (atom nil))

(defn- start-system []
  (let [system (or @system-state (system/new-system))]
    (reset! system-state (component/start-system system))))

(defn- stop-system []
  (component/stop-system @system-state))

(defn clear-database! []
  (let [datasource (-> (:db @system-state)
                       (select-keys [:datasource]))]
    (jdbc/db-do-commands datasource ["DROP SCHEMA IF EXISTS public CASCADE"
                                     "CREATE SCHEMA public"])))

(defn- init-test-file []
  (jdbc/with-db-transaction [datasource (:db @system-state)]
    (let [filename (str (UUID/randomUUID))
          file-key (str (UUID/randomUUID))
          conn     {:connection datasource}]
      (reset! metadata (metadata-store/create-file {:key          file-key
                                                    :filename     (str filename ".txt")
                                                    :content-type "text/plain"
                                                    :size         1}
                                                   conn))
      (reset! file (File/createTempFile filename "txt")))))

(defn- remove-test-file []
  (metadata-store/delete-file (:key @metadata) (:db @system-state))
  (io/delete-file @file))

(use-fixtures :once
  (fn [tests]
    (start-system)
    (tests)
    (clear-database!)
    (stop-system)))

(use-fixtures :each
  (fn [tests]
    (init-test-file)
    (try
      (tests)
      (finally
        (remove-test-file)))))

(deftest virus-scan-for-clean-file
  (let [db             (:db @system-state)
        storage-engine (:storage-engine @system-state)
        config         (:config @system-state)]
    (#'virus-scan/scan-files db storage-engine config)
    (let [metadata (test-metadata-store/get-metadata-for-tests [(:key @metadata)] db)]
      (is (= (:virus-scan-status metadata) "done")))))
