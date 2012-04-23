(ns demoapp.templates
  (:use [net.cgrand.enlive-html]))

(defn str->html [string]
  (-> string
      java.io.StringReader.
      html-resource
      (select [:body])
      first
      :content))

(defsnippet menu "menu.html" [:ul#menu]
  [menu-data]
  [:li] (clone-for [[name href] menu-data]
                   [:a] (set-attr :href href)
                   [:a] (content name)))

(def menu-data [["process URL" "/url"]
                ["process text" "/text"]])

(deftemplate start-page "base.html" []
  [:div#content] (content (menu menu-data)))

(deftemplate input-page "base.html" []
  [:h1] (content "Enter URL")
  [:div#content] (content (html-resource "article.html")))

(deftemplate text-input-page "base.html" []
  [:h1] (content "Enter text")
  [:div#content] (content (html-resource "text.html")))

(deftemplate result-page "result.html"
  [article locations]
  [:div#article :p] (content article)
  [:ul.tags :li] (clone-for [[i location] locations]
                            [:a] (content location)
                            [:a] (add-class (str "tag-" i)))
  [:form#tag-selection :input]
  (clone-for [[i _] locations]
             [:input] (set-attr :name (str "tag-" i))))
