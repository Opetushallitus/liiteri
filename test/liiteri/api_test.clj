(ns liiteri.api-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [liiteri.core :as system]))

(def system-state (atom nil))

(defn- start-server []
  (let [system (or @system-state (system/new-system))]
    (reset! system-state (component/start-system system))))

(defn- stop-server []
  (component/stop-system @system-state))

(use-fixtures :once
  (fn [tests]
    (start-server)
    (tests)
    (stop-server)))

(deftest test-app-starts-1
  (is (= 1 1)))
