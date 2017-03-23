(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.92"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "5.7.0"]
                 [clj-time "0.13.0"]
                 [metosin/compojure-api "1.1.10"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.flywaydb/flyway-core "4.1.1"]
                 [hikari-cp "1.7.5"]
                 [http-kit "2.2.0"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.postgresql/postgresql "9.4.1212"]
                 [prismatic/schema "1.1.3"]
                 [metosin/schema-tools "0.9.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [com.taoensso/timbre "4.8.0"]
                 [yesql "0.5.3"]
                 [ring-logger-timbre "0.7.5"]
                 [ch.qos.logback/logback-classic "1.2.2"]
                 [jarohen/chime "0.2.0"]]

  :profiles {:dev     {:dependencies   [[reloaded.repl "0.2.3"]]
                       :repl-options   {:init-ns user}
                       :source-paths   ["src" "dev-src"]
                       :resource-paths ["resources" "dev-resources"]
                       :plugins        [[lein-ancient "0.6.10"]]}
             :uberjar {:aot :all}}

  :main liiteri.core
  :target-path "target/%s")
