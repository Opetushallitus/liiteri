(ns liiteri.api-test
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [liiteri.config :as config]
            [liiteri.core :as system]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

(def system-state (atom nil))
(def config (config/new-config))

(defn- temp-dir []
  (-> (get-in config [:file-store :filesystem :base-path])
      (io/file)))

(defn- create-temp-dir []
  (.mkdirs (temp-dir)))

(defn- remove-temp-dir []
  (letfn [(remove-node [node]
            (when (.isDirectory node)
              (doseq [child-node (.listFiles node)]
                (remove-node child-node)))
            (io/delete-file node))]
    (remove-node (temp-dir))))

(defn- start-server []
  (let [system (or @system-state (system/new-system))]
    (reset! system-state (component/start-system system))))

(defn- stop-server []
  (component/stop-system @system-state))

(use-fixtures :once
  (fn [tests]
    (create-temp-dir)
    (start-server)
    (tests)
    (stop-server)
    (remove-temp-dir)))

(defn- file-stored? [file-key]
  (let [file (io/file (str (get-in config [:file-store :filesystem :base-path]) "/" file-key))]
    (.exists file)))

(deftest file-upload
  (let [file (io/file (io/resource "parrot.png"))
        path (str "http://localhost:" (get-in config [:server :port]) "/liiteri/api/files")
        resp @(http/post path {:multipart [{:name "file" :content file :filename "parrot.png"}]})
        body (json/parse-string (:body resp) true)]
    (is (= (:status resp) 200))
    (is (file-stored? (:key body)))
    (is (= (:filename body) "parrot.png"))
    (is (= (:content-type body) "application/octet-stream"))
    (is (= (:size body) 7777))
    (is (= (:deleted body) nil))
    (is (some? (:uploaded body)))))
