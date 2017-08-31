(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "5.8.0"]
                 [clj-time "0.14.0"]
                 [metosin/compojure-api "1.2.0-alpha5"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.flywaydb/flyway-core "4.2.0"]
                 [fi.vm.sade/auditlogger "5.0.0-SNAPSHOT"]
                 [hikari-cp "1.7.6"]
                 [http-kit "2.2.0"]
                 [org.clojure/java.jdbc "0.7.0"]
                 [org.postgresql/postgresql "42.1.3"]
                 [prismatic/schema "1.1.6"]
                 [metosin/schema-tools "0.9.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [com.taoensso/timbre "4.10.0"]
                 [ring-logger-timbre "0.7.5"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [yesql "0.5.3"]
                 [ring.middleware.conditional "0.2.0"]
                 [environ "1.1.0"]
                 [com.novemberain/pantomime "2.9.0"]
                 [jarohen/chime "0.2.2"]]

  :repositories [["snapshots" {:url "https://artifactory.oph.ware.fi/artifactory/oph-sade-snapshot-local"}]]

  :plugins [[lein-environ "1.1.0"]
            [lein-resource "16.11.1"]]

  :profiles {:dev        {:dependencies   [[reloaded.repl "0.2.3"]]
                          :repl-options   {:init-ns user}
                          :source-paths   ["src" "dev-src"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-ancient "0.6.10"]]
                          :env            {:config "dev-resources/dev-config.edn"}}

             :test-ci    {:test-paths            ["test"]
                          :resource-paths        ["resources" "dev-resources"]
                          :plugins               [[jonase/eastwood "0.2.3"]
                                                  [test2junit "1.2.5"]]
                          :test2junit-output-dir "target/test-reports"}

             :test-local {:test-paths     ["test"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-auto "0.1.3"]
                                           [jonase/eastwood "0.2.3"]]
                          :env            {:config "dev-resources/local-test-config.edn"}}

             :uberjar    {:aot :all}

             :db-schema  {:source-paths ["src" "db-schema-src"]
                          :main         liiteri.db-schema-diagram}}

  :resource {:resource-paths ["templates"]
             :target-path    "resources"
             :update         false
             :extra-values   {:version   "0.1.0-SNAPSHOT"
                              :buildTime ~(.format
                                            (java.text.SimpleDateFormat. "yyyyMMdd-HHmm")
                                            (java.util.Date.))
                              :githash   ~(System/getenv "githash")}
             :silent         false}

  :main liiteri.core

  :aliases {"test-local"      ["with-profile" "test-local" "test"]
            "test-ci"         ["with-profile" "test-ci" "test2junit"]
            "test-local-auto" ["with-profile" "test-local" "auto" "test"]
            "db-schema"       ["with-profile" "db-schema" "run"]}

  :uberjar-name "liiteri.jar")
