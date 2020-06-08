(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :managed-dependencies [[instaparse/instaparse "1.4.10"]
                         [org.apache.httpcomponents/httpclient "4.5.10"]
                         [org.apache.httpcomponents/httpcore "4.4.12"]
                         [commons-logging/commons-logging "1.2"]
                         [com.fasterxml.jackson.core/jackson-core "2.10.0"]
                         [com.fasterxml.jackson.core/jackson-databind "2.10.0"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.10.0"]
                         [riddley/riddley "0.2.0"]
                         [org.clojure/tools.reader "1.3.2"]
                         [org.apache.pdfbox/pdfbox "2.0.17"]]

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.641"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "5.9.0"]
                 [clj-time "0.15.2"]
                 [metosin/compojure-api "2.0.0-alpha30"]
                 [com.stuartsierra/component "0.4.0"]
                 [org.flywaydb/flyway-core "6.0.4"]
                 [fi.vm.sade/auditlogger "8.3.1-SNAPSHOT"]
                 [hikari-cp "2.9.0"]
                 [http-kit "2.3.0"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "42.2.8"]
                 [prismatic/schema "1.1.12"]
                 [metosin/schema-tools "0.12.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.14"]
                 [org.slf4j/slf4j-api "1.7.26"]
                 [org.slf4j/log4j-over-slf4j "1.7.26"]
                 [org.slf4j/jcl-over-slf4j "1.7.26"]
                 [yesql "0.5.3"]
                 [environ "1.1.0"]
                 [com.novemberain/pantomime "2.11.0"]
                 [org.apache.commons/commons-compress "1.19"]
                 [jarohen/chime "0.2.2"]
                 [clj-http "3.10.0"]
                 [aleph "0.4.6"]
                 [me.raynes/fs "1.4.6"]
                 [org.apache.pdfbox/pdfbox "2.0.17"]
                 [com.github.jai-imageio/jai-imageio-core "1.3.1"]
                 [com.github.jai-imageio/jai-imageio-jpeg2000 "1.3.0"]
                 [com.levigo.jbig2/levigo-jbig2-imageio "2.0"]
                 [oph/clj-access-logging "1.0.0-SNAPSHOT"]
                 [oph/clj-stdout-access-logging "1.0.0-SNAPSHOT"]
                 [oph/clj-timbre-access-logging "1.0.0-SNAPSHOT"]
                 [oph/clj-string-normalizer "0.1.0-SNAPSHOT"]]

  :repositories [["snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"}]]

  :plugins [[lein-environ "1.1.0"]
            [lein-resource "17.06.1"]]

  :profiles {:dev        {:dependencies   [[reloaded.repl "0.2.4"]]
                          :repl-options   {:init-ns user}
                          :source-paths   ["src" "dev-src"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-ancient "0.6.15"]]
                          :env            {:config "dev-resources/dev-config.edn"}}

             :test-ci    {:test-paths            ["test"]
                          :resource-paths        ["resources" "dev-resources"]
                          :plugins               [[jonase/eastwood "0.2.5"]
                                                  [test2junit "1.3.3"]]
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
