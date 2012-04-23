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
   ["" &] (delegate view-start-page)
   ["url" &] {:get (fn [_] (->> (input-page) response))
              :post (wrap-params view-results-page)}
   ["text" &] {:get (fn [_] (->> (text-input-page) response))
               :post (wrap-params view-results-page)}
   ["article"] {:get (delegate view-article-list)
                :post (wrap-params post-article)}
   ["article" id] {:get (delegate view-article id)}))

(defn -main [port obt-path]
  (do (init obt-path)
      (run-jetty #'routes {:port (Integer/parseInt port) :join? false})))

; (def server (-main "8082" "localhost:8085"))
