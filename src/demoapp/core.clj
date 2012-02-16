(ns demoapp.core
  (:use [demoapp controller]
        [ring.adapter.jetty]
        [ring.middleware resource reload file]
        [ring.util.response]
        [net.cgrand.moustache]))

(def api-key "AIzaSyA0IrRqPrqcuQTdbUS5o57-EsPEbFKRsOc")

(defn page [req]
  (response "This is page"))

(def routes
  (app
   [""] page
   ["map" &] (delegate map-page)))

(defonce server (run-jetty #'routes {:port 8080 :join? false}))
