(defproject demoapp "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.1.4"]
                 [clj-egsiona "0.1.3"]
                 [ring "1.0.2"]
                 [net.cgrand/moustache "1.1.0"]
                 [enlive "1.0.0"]
                 [geocoder-clj "0.0.5-SNAPSHOT"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [de.l3s.boilerpipe/boilerpipe "1.1.0"]
                 [xerces/xercesImpl "2.4.0"]
                 [net.sourceforge.nekohtml/nekohtml "1.9.15"]]
  :main demoapp.core)
