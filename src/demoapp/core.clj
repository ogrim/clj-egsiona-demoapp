(ns demoapp.core
  (:use [demoapp tools templates controller]
        [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response]
        [net.cgrand.moustache])
  (:gen-class :main true))

(defn init [obt-path]
  (configure-obt obt-path
                 {:classname   "org.sqlite.JDBC"
                  :subprotocol "sqlite"
                  :subname     "database.db"}))

(def routes
  (app
   (wrap-file "resources")
   ["" &] (fn [_] (->> (start-page) response))
   ["url" &] {:get (fn [_] (->> (input-page) response))
              :post (wrap-params view-results-page)}
   ["text" &] {:get (fn [_] (->> (text-input-page) response))
               :post (wrap-params view-results-page)}))

(defn -main [port obt-path]
  (do (init obt-path)
      (run-jetty #'routes {:port (Integer/parseInt port) :join? false})))
