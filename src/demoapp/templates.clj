(ns demoapp.templates
  (:use [net.cgrand.enlive-html]))

(defmacro maybe-content
  ([expr] `(if-let [x# ~expr] (content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

(defn str->html [string]
  (-> string
      java.io.StringReader.
      html-resource
      (select [:body])
      first
      :content))

(def start-menu-data [["insert by URL" "/url"]
                      ["insert by text" "/text"]
                      ["view articles" "/article"]])

(def top-menu-data [["Home" "/"]
                    ["Insert URL" "/url"]
                    ["Insert Text" "/text"]
                    ["View Articles" "/article"]])

(defsnippet menu "snippets.html" [:ul#menu]
  []
  [:li] (clone-for [[name href] start-menu-data]
                   [:a] (set-attr :href href)
                   [:a] (content name)))

(defsnippet top-menu "snippets.html" [:ul#top-menu]
  []
  [:li] (clone-for [[name href] top-menu-data]
                   [:a] (content name)
                   [:a] (set-attr :href href)))

(deftemplate base "base.html" [{:keys [data h1]}]
  [:ul#top-menu :li] (content (top-menu))
  [:h1]  (maybe-content h1)
  [:div#content] (maybe-content data))

(defn start-page [] (base {:data (menu)}))

(defn url-input-page []
  (base {:h1 "Enter URL"
         :data ((snippet "snippets.html" [:form#url-form] []))}))

(defn text-input-page []
  (base {:h1 "Enter text"
         :data ((snippet "snippets.html" [:form#text-form] []))}))

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
(defn article-list-page []
  (base {:h1 "Article list"}))
