(ns demoapp.core
  (:use [demoapp tools templates]
        [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response]
        [net.cgrand.moustache])
  (:require [demoapp.obt :as obt]))

(def api-key "AIzaSyA0IrRqPrqcuQTdbUS5o57-EsPEbFKRsOc")

(defn post-input-page [req]
  (response (apply str (map #(str % " - ") (obt/tag-article req)))))
(defn post-text-input-page [req]
  (response (apply str (map #(str % " - ") (obt/tag-text req)))))

(defn post-text-input-page [req]
  (response (str req)))

(defn view-result-page [req]
  (->> (result-page) response))

(defn view-start-page [req]
  (->> (start-page) response))

(def routes
  (app
   (wrap-file "resources")
;   (wrap-reload '[demoapp.templates])
   (wrap-reload ['demoapp.templates])
   [""] view-start-page
   ["url" &] {:get (response input-page)
              :post (wrap-params post-input-page)}
   ["text" &] {:get (response text-input-page)
               :post (wrap-params post-text-input-page)}
   ["map" &] (delegate map-page)
   ["result" &] view-result-page))

(defonce server (run-jetty #'routes {:port 8081 :join? false}))
