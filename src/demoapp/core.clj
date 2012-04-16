(ns demoapp.core
  (:use [demoapp tools templates]
        [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response]
        [net.cgrand.moustache])
  (:require [demoapp.obt :as obt]))

(def api-key "AIzaSyA0IrRqPrqcuQTdbUS5o57-EsPEbFKRsOc")

(defn view-results-page [req]
  (let [params (:form-params req)
        [text locations] (process-params params)]
    (->> (result-page text locations) response)))

(defn process-params [params]
  (if-let [url (get params "url")]
    (let [content (obt/extract-content url)]
      [content (obt/process-text content)])
    (let [content (get params "text")]
      [content (obt/process-text content)])))

(def routes
  (app
   (wrap-file "resources")
   [""] (fn [_] (->> (start-page) response))
   ["url" &] {:get (fn [_] (->> (input-page) response))
              :post (wrap-params view-results-page)}
   ["text" &] {:get (fn [_] (->> (text-input-page) response))
               :post (wrap-params view-results-page)}
   ["map" &] (delegate map-page)))

(defonce server (run-jetty #'routes {:port 8081 :join? false}))
