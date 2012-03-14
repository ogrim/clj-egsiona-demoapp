(ns demoapp.controller
  (:use [ring.util.response]
        [demoapp.templates]))

(defn map-page [req]
  (->> (slurp "./resources/test.html") response))

(defn input-page [req]
  (->> (slurp "./resources/article.html") response))

(defn start-page [req]
  (->> (slurp "./resources/start.html") response))

(defn text-input-page [req]
  (->> (slurp "./resources/text.html") response))
