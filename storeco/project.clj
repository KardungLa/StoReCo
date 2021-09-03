(defproject storeco "0.2.0-SNAPSHOT"
  :description "Enhanced Stand-off TEI Annotation with StoReCo: A generic approach with the use of RDF."
  :url "https://github.com/KardungLa/StoReCo"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
    [org.clojure/clojure "1.10.1"]
    [org.clojure/data.json "2.2.2"]
    [org.clojure/tools.logging "1.1.0"]
    [clj-http/clj-http "3.12.0"]
    [clojure-tools/clojure-tools "1.1.3"]
    [org.clojure/tools.cli "1.0.206"]
    [org.apache.jena/jena-core "3.17.0"]
    [org.apache.jena/jena-arq "3.17.0"]
    [org.apache.jena/apache-jena-libs "3.17.0" :extension "pom"]
    [net.rootdev/java-rdfa "1.0.0-BETA1"]
    [com.franz/openrdf-sesame-onejar "2.3.1"]
    [nu.validator.htmlparser/htmlparser "1.2.1"]
    [midje/midje "1.9.10"]
    [lein-midje/lein-midje "3.2.2"]
    [org.clojure/data.xml "0.2.0-alpha6"]
    [org.clojure/data.zip "1.0.0"]
    [tolitius/xml-in "0.1.1"]
    [cheshire "5.10.0"]
    [enlive "1.1.6"]
    [metosin/maailma "1.1.0"]
    ]
  :main at.danielschlager.storeco.core
  :target-path "target/%s"
  :jvm-opts ["-Xmx1g" "-server"]
  :profiles {:uberjar {:aot :all}})
