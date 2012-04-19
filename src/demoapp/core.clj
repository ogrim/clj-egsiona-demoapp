(ns demoapp.core
  (:use [demoapp tools templates controller]
        [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response]
        [net.cgrand.moustache]))

(def api-key "AIzaSyA0IrRqPrqcuQTdbUS5o57-EsPEbFKRsOc")

(configure-obt "localhost:8085"
               {:classname   "org.sqlite.JDBC"
                :subprotocol "sqlite"
                :subname     "database.db"})

(def routes
  (app
   (wrap-file "resources")
   ["" &] (fn [_] (->> (start-page) response))
   ["url" &] {:get (fn [_] (->> (input-page) response))
              :post (wrap-params view-results-page)}
   ["text" &] {:get (fn [_] (->> (text-input-page) response))
               :post (wrap-params view-results-page)}
   ["map" &] (delegate map-page)))

(defonce server (run-jetty #'routes {:port 8081 :join? false}))
