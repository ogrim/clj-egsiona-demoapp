(ns demoapp.controller
  (:use [ring.util.response]
        [ring.middleware.reload]
        [demoapp.templates]
        [net.cgrand.enlive-html])
  (:require [demoapp.obt :as obt]))

(defn map-page [req]
  (->> (slurp "./resources/test.html") response))

(defn input-page [req]
  (->> (slurp "./resources/article.html") response))

(defn start-page [req]
  (->> (slurp "./resources/start.html") response))

(defn text-input-page [req]
  (->> (slurp "./resources/text.html") response))

(defn post-input-page [req]
  (response (apply str (map #(str % " - ") (obt/tag-article req)))))

(defn post-text-input-page [req]
  (response (apply str (map #(str % " - ") (obt/tag-text req)))))

(defn post-text-input-page [req]
  (response (str req)))

(defn view-result-page [req]
  (response (result-page)))



(comment (defn result-page [req]
   (->> (slurp "./resources/result.html") response)))
