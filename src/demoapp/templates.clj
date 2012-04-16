(ns demoapp.templates
  (:use [net.cgrand.enlive-html]))

(deftemplate start-page "start.html" [])

(deftemplate input-page "article.html" [])

(deftemplate text-input-page "text.html" [])

(deftemplate map-page "test.html" [])

(deftemplate result-page "result.html"
  [article locations]
  [:div#article :p] (content article)
  [:div#locations :ul :li] (clone-for [location locations]
                                      [:a] (content location)))
