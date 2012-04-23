(ns demoapp.templates
  (:use [net.cgrand.enlive-html]))

(defn str->html [string]
  (-> string
      java.io.StringReader.
      html-resource
      (select [:body])
      first
      :content))

(def start-menu-data [["insert from URL" "/url"]
                      ["insert by text" "/text"]
                      ["view articles" "/article"]])

(def top-menu-data [["Home" "/"]
                    ["Insert URL" "/url"]
                    ["Insert Text" "/text"]
                    ["View Articles" "/article"]])

(defsnippet menu "menu.html" [:ul#menu]
  []
  [:li] (clone-for [[name href] menu-data]
                   [:a] (set-attr :href href)
                   [:a] (content name)))

(defsnippet top-menu "menu.html" [:ul#top-menu]
  []
  [:li] (clone-for [[name href] top-menu-data]
                   [:a] (content name)
                   [:a] (set-attr :href href)))

(deftemplate start-page "base.html" []
  [:ul#top-menu :li] (content (top-menu))
  [:div#content] (content (menu)))

(deftemplate input-page "base.html" []
  [:h1] (content "Enter URL")
  [:ul#top-menu :li] (content (top-menu))
  [:div#content] (content (html-resource "article.html")))

(deftemplate text-input-page "base.html" []
  [:h1] (content "Enter text")
  [:ul#top-menu :li] (content (top-menu))
  [:div#content] (content (html-resource "text.html")))

(deftemplate result-page "result.html"
  [article locations]
  [:h1] (content "Suggested locations")
  [:ul#top-menu :li] (content (top-menu))
  [:div#article :p] (content article)
  [:ul.tags :li] (clone-for [[i location] locations]
                            [:a] (content location)
                            [:a] (add-class (str "tag-" i)))
  [:form#tag-selection :input.tag-hidden]
  (clone-for [[i _] locations]
             [:input.tag-hidden] (set-attr :name (str "tag-" i))))

(deftemplate article-page "single.html"
  [article locations]
  [:h1] (content "View article")
  [:ul#top-menu :li] (content (top-menu))
  [:div#article :p] (content article)
  [:ul.tags :li] (clone-for [[i location] locations]
                            [:a] (content location)
                            [:a] (add-class (str "tag-" i))))
