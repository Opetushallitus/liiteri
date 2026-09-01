;; Riippuvuusperheiden versiot yhtenä totuutena. Leiningen ei tue Maven-BOM importia
;; (:scope "import"), joten artefaktit listataan eksplisiittisesti mutta versio jaetaan muuttujalla.
(def jackson-version "2.21.6")     ; CVE-2026-54512/54513 (CRITICAL); 2.19/2.20-linjoille ei backporttia
(def netty-version "4.2.17.Final") ; CVE-2026-44249, CVE-2026-75595 (CRITICAL) ym.; java-cas 2.3.0 / AHC 3.0.12
(def bouncycastle-version "1.85")  ; CVE-2026-8763 ym. (CRITICAL)

(defproject liiteri "0.1.0-SNAPSHOT"
  :description "File Storage Service For OPH"
  :url "https://github.com/Opetushallitus/liiteri"

  :licence {:name "EUPL"
            :url  "https://www.tldrlegal.com/l/eupl-1.1"}

  :managed-dependencies [[instaparse/instaparse "1.4.10"]
                         [riddley/riddley "0.2.0"]
                         [org.clojure/tools.reader "1.3.2"]
                         [com.typesafe.akka/akka-actor_2.12 "2.5.32"]
                         [com.typesafe.akka/akka-http-core_2.12 "10.1.15"]
                         [commons-fileupload/commons-fileupload "1.6.0"]
                         [org.apache.commons/commons-fileupload2-core "2.0.0-M4"]
                         [com.google.code.gson/gson "2.11.0"]
                         [com.google.protobuf/protobuf-java "3.25.5"]
                         [com.google.guava/guava "33.3.0-jre"]
                         [clj-commons/clj-yaml "1.0.27"]
                         [org.yaml/snakeyaml "2.2"]
                         [org.apache.pdfbox/pdfbox "3.0.5"]
                         ;; Tietoturvapäivitykset 2026-09 (transitiiviset pinnit)
                         [com.fasterxml.jackson.core/jackson-core ~jackson-version]
                         [com.fasterxml.jackson.core/jackson-databind ~jackson-version]
                         [com.fasterxml.jackson.core/jackson-annotations "2.21"] ; annotations-linja ei käytä patch-numeroa
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor ~jackson-version]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-smile ~jackson-version]
                         [org.mozilla/rhino "1.7.15.1"]
                         [org.jsoup/jsoup "1.23.2"]
                         [com.github.junrar/junrar "7.5.10"]
                         [org.bouncycastle/bcjmail-jdk18on ~bouncycastle-version]
                         ;; auditlogger 9.2.7 tuo 3.13.0 (CVE-2025-48924)
                         [org.apache.commons/commons-lang3 "3.20.0"]
                         ;; Pidä koko io.netty-perhe yhdessä versiossa. java-cas 2.3.0 -> AHC 3.0.12
                         ;; vaatii netty 4.2.x (netty-codec-base/-compression); aws-sdk toisi 4.1.126.
                         ;; ~netty-version tiedoston alussa (kattaa kaikki netty-CVE:t, sis. AHC 3.0.12).
                         [io.netty/netty-buffer ~netty-version]
                         [io.netty/netty-common ~netty-version]
                         [io.netty/netty-codec ~netty-version]
                         [io.netty/netty-codec-base ~netty-version]
                         [io.netty/netty-codec-compression ~netty-version]
                         [io.netty/netty-codec-dns ~netty-version]
                         [io.netty/netty-codec-http ~netty-version]
                         [io.netty/netty-codec-http2 ~netty-version]
                         [io.netty/netty-codec-socks ~netty-version]
                         [io.netty/netty-handler ~netty-version]
                         [io.netty/netty-handler-proxy ~netty-version]
                         [io.netty/netty-resolver ~netty-version]
                         [io.netty/netty-resolver-dns ~netty-version]
                         [io.netty/netty-transport ~netty-version]
                         [io.netty/netty-transport-classes-epoll ~netty-version]
                         [io.netty/netty-transport-classes-kqueue ~netty-version]
                         [io.netty/netty-transport-native-unix-common ~netty-version]
                         [io.netty/netty-transport-native-epoll ~netty-version :classifier "linux-x86_64"]
                         [io.netty/netty-transport-native-epoll ~netty-version :classifier "linux-aarch_64"]
                         [io.netty/netty-transport-native-kqueue ~netty-version :classifier "osx-x86_64"]]

  :dependencies [[org.clojure/clojure "1.11.2"]
                 [software.amazon.awssdk/s3 "2.37.3"]
                 [software.amazon.awssdk/sqs "2.37.3"]
                 [camel-snake-kebab "0.4.0"]
                 [cheshire "6.1.0"]
                 [metosin/compojure-api "1.1.14"]
                 [com.stuartsierra/component "0.4.0"]
                 [org.flywaydb/flyway-core "6.0.4"]
                 ;; 8.3.1-SNAPSHOT (2019) oli vain Artifactoryssa -> 9.2.7-SNAPSHOT (GitHub Packages,
                 ;; sama kuin ataru/maksut; API yhteensopiva liiterin audit_log.clj:n kanssa)
                 [fi.vm.sade/auditlogger "9.2.7-SNAPSHOT"]
                 [hikari-cp "2.9.0"]
                 [http-kit "2.8.1"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "42.7.12"]
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
                 [opiskelijavalinnat-utils/java-cas "2.3.0-SNAPSHOT"]
                 ;; buddy-core 1.12 siirtyy bcprov/bcpkix/bcutil-jdk18on-linjalle (pois haavoittuvasta *-jdk15on 1.70:stä)
                 [buddy/buddy-core "1.12.0-430"]
                 [org.apache.tika/tika-core "3.2.3"]
                 [org.bouncycastle/bcprov-jdk18on "1.85"]
                 [org.bouncycastle/bcpkix-jdk18on "1.85"]
                 [org.bouncycastle/bcutil-jdk18on "1.85"]
                 ; Exclusions to get rid warnings about version ranges.
                 [org.apache.tika/tika-parsers-standard-package "3.2.3" :exclusions [org.bouncycastle/bcpkix-jdk18on org.bouncycastle/bcutil-jdk18on org.bouncycastle/bcprov-jdk18on]]
                 [org.apache.commons/commons-compress "1.27.1"]
                 [commons-io/commons-io "2.19.0"]
                 [jarohen/chime "0.2.2"]
                 [clj-http "3.13.1"]
                 [aleph "0.9.11"]
                 ;; me.raynes/fs 1.4.6:lle ei tietoturvakorjausta -> ylläpidetty fork, sama namespace (me.raynes.fs)
                 [clj-commons/fs "1.6.312"]
                 [org.apache.pdfbox/pdfbox "3.0.5"]
                 [com.github.jai-imageio/jai-imageio-core "1.4.0"]
                 [com.github.jai-imageio/jai-imageio-jpeg2000 "1.4.0"]
                 [com.twelvemonkeys.imageio/imageio-jpeg "3.11.0"]
                 [org.apache.pdfbox/jbig2-imageio "3.0.4"]
                 [ring/ring-session-timeout "0.2.0"]
                 [opiskelijavalinnat-utils/clj-ring-db-cas-session "1.0.0-SNAPSHOT"]]
                 ;; oph/clj-*-access-logging olivat vain Artifactoryssa (poistumassa käytöstä).
                 ;; Nämä pienet yhden tiedoston kirjastot on vendoroitu src/liiteri/clj_*_access_logging.clj:hin
                 ;; (sama kuin ataru/maksut/hakukohderyhmapalvelu tekevät).

  ;; Artifactory poistettu -> kaikki SNAPSHOTit GitHub Packagesista
  :repositories [["github" {:url "https://maven.pkg.github.com/Opetushallitus/packages"
                            :username "private-token"
                            :password :env/GITHUB_TOKEN}]]

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
