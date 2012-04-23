(ns demoapp.templates
  (:use [net.cgrand.enlive-html]))

(defmacro maybe-content
  ([expr] `(if-let [x# ~expr] (content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

(defmacro maybe-substitute
  ([expr] `(if-let [x# ~expr] (substitute x#) identity))
  ([expr & exprs] `(maybe-substitute (or ~expr ~@exprs))))

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

(defsnippet iso-8859-1 "snippets.html" [:meta] [])

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

(deftemplate base "base.html" [{:keys [h1 main meta]}]
  [:meta] (maybe-substitute meta)
  [:ul#top-menu :li] (content (top-menu))
  [:h1]  (maybe-content h1)
  [:div#content] (maybe-content main))

(defn start-page [] (base {:main (menu)}))

(defn url-input-page []
  (base {:h1 "Enter URL"
         :main ((snippet "snippets.html" [:form#url-form] []))}))

(defn text-input-page []
  (base {:h1 "Enter text"
         :main ((snippet "snippets.html" [:form#text-form] []))}))

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

(defsnippet article-list "snippets.html" [:ul#article-list]
  [articles]
  [:li] (clone-for [{:keys [i tagcount preview]} articles]
                   [[:a (nth-of-type 1)]] (set-attr :href (str "/article/" i))
                   [[:a (nth-of-type 1)]] (content (str tagcount))
                   [[:a (nth-of-type 2)]] (set-attr :href (str "/article/" i))
                   [[:a (nth-of-type 2)]] (content preview)))

(defn article-list-page [articles]
  (base {:h1 "Article list"
         :main (article-list articles)
         :meta (iso-8859-1)}))
