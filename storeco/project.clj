(defproject storeco "0.1.0-SNAPSHOT"
  :description "Enhanced Stand-off TEI Annotation with StoReCo: A generic approach with the use of RDF."
  :url "https://github.com/KardungLa/StoReCo"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"] [org.clojure/tools.cli "0.4.2"] [net.kronkltd/plaza "0.3.0-SNAPSHOT"] [nu.validator.htmlparser/htmlparser "1.2.1"] [org.clojure/data.xml "0.2.0-alpha6"] [org.clojure/data.zip "1.0.0"] [tolitius/xml-in "0.1.1"] [cheshire "5.10.0"] [enlive "1.1.6"] [metosin/maailma "1.1.0"] [rhizome "0.2.9"]]
  :main ^:skip-aot storeco.core
  :target-path "target/%s"
  :jvm-opts ["-Xmx1g" "-server"]
  :profiles {:uberjar {:aot :all}})
