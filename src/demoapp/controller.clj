(ns demoapp.controller
  (:use [ring.util.response]
        [demoapp.templates]))

(defn map-page [req]
  (->> (slurp "./resources/test.html") response))

(defn input-page [req]
  (->> (slurp "./resources/article.html") response))
