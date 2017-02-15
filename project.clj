(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.stuartsierra/component "0.3.2"]]

  :profiles {:dev     {:dependencies [[reloaded.repl "0.2.3"]]
                       :repl-options {:init-ns user}
                       :source-paths ["src" "dev-src"]}
             :uberjar {:aot :all}}

  :main liiteri.core
  :target-path "target/%s")
