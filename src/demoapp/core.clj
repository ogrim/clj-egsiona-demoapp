(ns demoapp.core
  (:use [demoapp controller]
        [ring.adapter.jetty]
        [ring.middleware resource reload file]
        [ring.util.response]
        [net.cgrand.moustache])
  (:require [clj-egsiona.core :as e]))

(def api-key "AIzaSyA0IrRqPrqcuQTdbUS5o57-EsPEbFKRsOc")

(defn page [req]
  (response "This is page"))

(def routes
  (app
   [""] page
   ["map" &] (delegate map-page)))

(defonce server (run-jetty #'routes {:port 8080 :join? false}))

(e/set-obt "/home/ogrim/bin/The-Oslo-Bergen-Tagger")

(comment (e/process-text "Tror du at Sandnes er en lokasjon?"))

(comment (e/set-db {:classname "org.postgresql.Driver"
            :subprotocol "postgresql"
            :subname  "//localhost:5432/ogrimtest"
            :user "postgres"
            :password "babbafet"}))



(e/set-db {:classname   "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname     "database.db"})

(set-db {:classname   "org.sqlite.JDBC"
         :subprotocol "sqlite"
         :subname     "database.db"})

;(e/create-tables)
;(e/process-text "Tror du at Sandnes er en lokasjon?")
