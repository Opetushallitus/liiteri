(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.7.0"]
                 [metosin/compojure-api "1.1.10"]
                 [com.stuartsierra/component "0.3.2"]
                 [hikari-cp "1.7.5"]
                 [http-kit "2.2.0"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.postgresql/postgresql "9.4.1212"]
                 [prismatic/schema "1.1.3"]
                 [javax.servlet/servlet-api "2.5"]
                 [com.taoensso/timbre "4.8.0"]]

  :profiles {:dev     {:dependencies   [[reloaded.repl "0.2.3"]]
                       :repl-options   {:init-ns user}
                       :source-paths   ["src" "dev-src"]
                       :resource-paths ["resources" "dev-resources"]}
             :uberjar {:aot :all}}

  :main liiteri.core
  :target-path "target/%s")
