(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :managed-dependencies [[instaparse/instaparse "1.4.10"]
                         [org.apache.httpcomponents/httpclient "4.5.14"]
                         [org.apache.httpcomponents/httpcore "4.4.16"]
                         [org.apache.httpcomponents/httpmime "4.5.14"]
                         [com.fasterxml.jackson.core/jackson-core "2.18.1"]
                         [com.fasterxml.jackson.core/jackson-databind "2.18.1"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.18.1"]
                         [riddley/riddley "0.2.0"]
                         [org.clojure/tools.reader "1.3.2"]
                         [io.netty/netty-codec-http "4.1.127.Final"] ; fixes CVE-2021-21295
                         [io.netty/netty-common "4.1.127.Final"]
                         [io.netty/netty-buffer "4.1.127.Final"]
                         [io.netty/netty-transport "4.1.127.Final"]
                         [io.netty/netty-codec "4.1.127.Final"]
                         [io.netty/netty-handler "4.1.127.Final"]
                         [io.netty/netty-resolver "4.1.127.Final"]
                         [com.typesafe.akka/akka-actor_2.12 "2.5.32"]
                         [io.undertow/undertow-core "2.3.19.Final"]
                         [org.jboss.xnio/xnio-api "3.8.14.Final"]
                         [org.jboss.xnio/xnio-nio "3.8.14.Final"]
                         [org.eclipse.jetty/jetty-server "9.4.57.v20241219"]
                         [org.eclipse.jetty/jetty-io "9.4.57.v20241219"]
                         [org.eclipse.jetty/jetty-http "9.4.57.v20241219"]
                         [org.eclipse.jetty/jetty-util "9.4.57.v20241219"]
                         [org.http4s/blaze-core_2.12 "0.23.14"]
                         [commons-fileupload/commons-fileupload "1.6.0"]
                         [com.google.code.gson/gson "2.11.0"]
                         [com.google.protobuf/protobuf-java "3.25.5"]
                         [com.google.guava/guava "33.3.0-jre"]
                         [org.yaml/snakeyaml "2.2"]
                         [org.apache.pdfbox/pdfbox "3.0.3"]]

  :dependencies [[org.clojure/clojure "1.11.2"]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.740" :exclusions [commons-logging/commons-logging]]
                 [com.amazonaws/aws-java-sdk-sqs "1.12.740" :exclusions [commons-logging/commons-logging]]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "6.1.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [clj-commons/clj-yaml "1.0.27" :exclusions [org.yaml/snakeyaml]]
                 [org.yaml/snakeyaml "2.2"]
                 [clj-time "0.15.2"]
                 [metosin/compojure-api "1.1.14"]
                 [com.stuartsierra/component "0.4.0"]
                 [org.flywaydb/flyway-core "6.0.4"]
                 [fi.vm.sade/auditlogger "8.3.1-SNAPSHOT"]
                 [hikari-cp "2.9.0"]
                 [http-kit "2.8.1"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "42.7.3"]
                 [prismatic/schema "1.1.12"]
                 [metosin/schema-tools "0.12.0"]
                 [javax.servlet/javax.servlet-api "4.0.1" :scope "provided"]
                 [com.taoensso/timbre "6.6.1"]
                 [timbre-ns-pattern-level "0.1.2"]
                 [com.fzakaria/slf4j-timbre "0.4.1"]
                 [org.slf4j/slf4j-api "1.7.36"]
                 [org.slf4j/log4j-over-slf4j "1.7.36"]
                 [org.slf4j/jcl-over-slf4j "1.7.36"]
                 [yesql "0.5.3"]
                 [environ "1.1.0"]
                 [fi.vm.sade/scala-cas_2.12 "2.2.2.1-SNAPSHOT"]
                 [org.apache.tika/tika-core "3.2.3"]
                 [org.apache.tika/tika-parsers-standard-package "3.2.3"]
                 [org.bouncycastle/bcprov-jdk18on "1.82"]
                 [org.bouncycastle/bcpkix-jdk18on "1.82"]
                 [org.apache.commons/commons-compress "1.27.1"]
                 [commons-io/commons-io "2.19.0"]
                 [jarohen/chime "0.2.2"]
                 [clj-http "3.13.1" :exclusions [commons-logging/commons-logging]]
                 ; Updating to aleph 0.7.1 will break ECR deployments. This is because it includes Brotli support using
                 ; native libraries which (if I understand correctly) assume that the system is glibc, not musl, based
                 [aleph "0.6.4"]
                 [me.raynes/fs "1.4.6"]
                 [org.apache.pdfbox/pdfbox "3.0.3"]
                 [com.github.jai-imageio/jai-imageio-core "1.4.0"]
                 [com.github.jai-imageio/jai-imageio-jpeg2000 "1.4.0"]
                 [com.twelvemonkeys.imageio/imageio-jpeg "3.11.0"]
                 [org.apache.pdfbox/jbig2-imageio "3.0.4"]
                 [ring/ring-session-timeout "0.2.0"]
                 [oph/clj-ring-db-cas-session "0.3.0-SNAPSHOT"]
                 [oph/clj-access-logging "1.0.0-SNAPSHOT"]
                 [oph/clj-stdout-access-logging "1.0.0-SNAPSHOT"]
                 [oph/clj-timbre-access-logging "1.1.0-SNAPSHOT"]
                 [oph/clj-string-normalizer "0.1.0-SNAPSHOT"]]

  :repositories [["snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"}]]

  :plugins [[lein-environ "1.1.0"]
            [lein-resource "17.06.1"]]

  :env     {:dev? "true"
            :aws-access-key "localstack"
            :aws-secret-key "localstack"}

  :profiles {:dev        {:dependencies   [[reloaded.repl "0.2.4"]]
                          :repl-options   {:init-ns user}
                          :source-paths   ["src" "dev-src"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-ancient "0.6.15"]]
                          :env            {:dev? "true"
                                           :config "dev-resources/dev-config.edn"}}

             :test-ci    {:test-paths            ["test"]
                          :resource-paths        ["resources" "dev-resources"]
                          :plugins               [[jonase/eastwood "0.2.5"]
                                                  [test2junit "1.3.3"]]
                          :env            {:dev? "true"}
                          :test2junit-output-dir "target/test-reports"}

             :test-local {:test-paths     ["test"]
                          :resource-paths ["resources" "dev-resources"]
                          :plugins        [[lein-auto "0.1.3"]
                                           [jonase/eastwood "0.2.3"]]
                          :env            {:dev? "true"
                                           :config "dev-resources/local-test-config.edn"}}

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
