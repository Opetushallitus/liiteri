(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "5.7.0"]
                 [clj-time "0.13.0"]
                 [metosin/compojure-api "1.1.10"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.flywaydb/flyway-core "4.1.2"]
                 [hikari-cp "1.7.5"]
                 [http-kit "2.2.0"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.postgresql/postgresql "42.0.0"]
                 [prismatic/schema "1.1.5"]
                 [metosin/schema-tools "0.9.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [com.taoensso/timbre "4.8.0"]
                 [ring-logger-timbre "0.7.5"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [yesql "0.5.3"]
                 [ring.middleware.conditional "0.2.0"]
                 [environ "1.1.0"]
                 [com.novemberain/pantomime "2.9.0"]
                 [jarohen/chime "0.2.0"]]

  :plugins [[lein-environ "1.1.0"]
            [lein-resource "16.11.1"]]

  :profiles {:dev        {:dependencies   [[reloaded.repl "0.2.3"]]
                          :repl-options   {:init-ns user}
                          :source-paths   ["src" "dev-src"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-ancient "0.6.10"]]
                          :env            {:config "dev-resources/dev-config.edn"}}

             :test-ci    {:test-paths     ["test"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-auto "0.1.3"]
                                           [jonase/eastwood "0.2.3"]]
                          :env            {:config "dev-resources/circleci-config.edn"}}

             :test-local {:test-paths     ["test"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-auto "0.1.3"]
                                           [jonase/eastwood "0.2.3"]]
                          :env            {:config "dev-resources/local-test-config.edn"}}

             :uberjar    {:aot :all}}

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
            "test-ci"         ["with-profile" "test-ci" "test"]
            "test-local-auto" ["with-profile" "test-local" "auto" "test"]}

  :eastwood {:namespaces      [:source-paths]
             :exclude-linters [:suspicious-expression]}

  :uberjar-name "liiteri.jar")