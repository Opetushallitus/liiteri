(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [metosin/compojure-api "1.1.10"]
                 [com.stuartsierra/component "0.3.2"]
                 [http-kit "2.2.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [com.taoensso/timbre "4.8.0"]]

  :profiles {:dev     {:dependencies [[reloaded.repl "0.2.3"]]
                       :repl-options {:init-ns user}
                       :source-paths ["src" "dev-src"]}
             :uberjar {:aot :all}}

  :main liiteri.core
  :target-path "target/%s")
