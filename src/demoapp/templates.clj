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

(defsnippet google-map-script "snippets.html" [:script] [api-key]
  [:script] (set-attr :src (str "http://maps.googleapis.com/maps/api/js?key="
                                api-key "&sensor=false")))

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

(comment (defsnippet map-view "maps.html" [:script] [geocoded]
   (let [f (first geocoded)
         c (format "function initialize() {
             var myOptions = {
               center: new google.maps.LatLng(%s, %s),
               zoom: 8,
               mapTypeId: google.maps.MapTypeId.ROADMAP
          };
          var map = new google.maps.Map(document.getElementById(\"map_canvas\"),
            myOptions);
          }" (:lat f) (:lon f))])
   (substitute geocoded)))

(comment (defn test1 [lat lon]
   (format "function initialize() {
             var myOptions = {
               center: new google.maps.LatLng(%s, %s),
               zoom: 8,
               mapTypeId: google.maps.MapTypeId.ROADMAP
          };
          var map = new google.maps.Map(document.getElementById(\"map_canvas\"),
            myOptions);
          }" lat lon)))

(comment (snippet "maps.html" [:script] [geocoded]
          (let [f (first geocoded)
                c (format "function initialize() {
             var myOptions = {
               center: new google.maps.LatLng(%s, %s),
               zoom: 8,
               mapTypeId: google.maps.MapTypeId.ROADMAP
          };
          var map = new google.maps.Map(document.getElementById(\"map_canvas\"),
            myOptions);
          }" (:lat f) (:lon f))])
          (substitute geocoded)))

(deftemplate map-page "single.html" [{:keys [api-key article locations geocoded]}]
  [:head] (content (conj (iso-8859-1)
                         (first ((snippet "snippets.html" [:link] [])))
                         (first (google-map-script api-key))
                         ;((snippet "maps.html" [:script] [s] (content s)) "-34.397" "150.644")
                         (first ((snippet "maps.html" [:script] [])))))
  [:ul#top-menu :li] (content (top-menu))
  [:body] (set-attr :onload "initialize()")
  [:h1]  (content "View article")
  [:div#article :p] (content article)
;  [:div#article :p]  (content (apply str geocoded))
  [:ul.tags :li] (clone-for [{:keys [i name]} geocoded];[[i name] locations];[{:keys [i name lat lon]} geocoded]
                            [:a] (comp (content name) (add-class (str "tag-" i)))
))

(defn str-loc [loc]
  (str (:name loc) " lat:" (:lat loc) " - lon:" (:lon loc) "  \n"))

(deftemplate map-page "single.html" [{:keys [api-key article locations geocoded]}]
  [:head] (content (conj (iso-8859-1)
                         (first ((snippet "snippets.html" [:link] [])))
                         (first (google-map-script api-key))
                         ;((snippet "maps.html" [:script] [s] (content s)) "-34.397" "150.644")
                         (first ((snippet "maps.html" [:script] [])))))
  [:ul#top-menu :li] (content (top-menu))
  [:body] (set-attr :onload "initialize()")
  [:h1]  (content "View article")
;  [:div#article :p] (content article)
  [:div#article :p]  (content (map str-loc geocoded))
  [:ul.tags :li] (clone-for [{:keys [i name]} geocoded]
                            [:a] (comp (content name) (add-class (str "tag-" i)))))


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
                            [:a] (comp (content location) (add-class (str "tag-" i))))
  [:form#tag-selection :input.tag-hidden]
  (clone-for [[i _] locations]
             [:input.tag-hidden] (set-attr :name (str "tag-" i))))

(deftemplate article-page "single.html"
  [article locations]
  [:h1] (content "View article")
  [:ul#top-menu :li] (content (top-menu))
  [:div#article :p] (content article)
  [:ul.tags :li] (clone-for [[i location] locations]
                            [:a] (comp (content location) (add-class (str "tag-" i)))))

(defsnippet article-list "snippets.html" [:ul#article-list]
  [articles]
  [:li] (clone-for [{:keys [i tagcount preview]} articles]
                   [[:a (nth-of-type 1)]] (comp (set-attr :href (str "/article/" i))
                                                (content (str tagcount)))
                   [[:a (nth-of-type 2)]] (comp (set-attr :href (str "/article/" i))
                                                (content preview))))

(defn article-list-page [articles]
  (base {:h1 "Article list"
         :main (article-list articles)
         :meta (iso-8859-1)}))
