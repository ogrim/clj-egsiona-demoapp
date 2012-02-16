(ns demoapp.controller
  (:use [ring.util.response]
        [demoapp.templates]))

(defn map-page [req]
  (->> (slurp "./resources/test.html") response))
