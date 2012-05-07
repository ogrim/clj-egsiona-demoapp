(ns demoapp.templates
  (:use [net.cgrand.enlive-html]))

(def ^{:private true} default-loc {:lat "60.3912628" :lon "5.3220544"})

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

(defn make-marker [{:keys [i lat lon name]}]
  (str "var position" i " = new google.maps.LatLng(" lat ", " lon ");
        bounds.extend(position" i ");

        var marker" i " = new google.maps.Marker({
             position: position" i ",
             title:\"" name "\"
        });

        var infostring" i " = '"name" <br /> latitude: " lat " <br /> longitude: " lon"';

        var infowindow" i " = new google.maps.InfoWindow({
          content: infostring" i "
        });

        marker" i ".setMap(map);

        google.maps.event.addListener(marker"i", 'click', function() {
          infowindow"i".open(map,marker"i");
        });
"))

(defn generate-script [geocoded]
  (let [[loc & locs :as locations] (filter #(seq (:lat %)) geocoded)
        center (if (nil? loc) default-loc loc)
        c (format "
            var centerloc = new google.maps.LatLng(%s, %s);
            function initialize() {
             var myOptions = {
               center: centerloc,
               zoom: 10,
               mapTypeId: google.maps.MapTypeId.ROADMAP
          };
          var map = new google.maps.Map(document.getElementById(\"map_canvas\"), myOptions);
          var bounds = new google.maps.LatLngBounds();
          bounds.extend(centerloc);

          %s

          map.fitBounds(bounds);
          }" (:lat center) (:lon center) "";(->> (map make-marker locations) (apply str))
           )]
    c))

(defsnippet construct-map "maps.html" [:script] [geocoded]
  (let [script (generate-script geocoded)]
    (content script)))

(defn str-loc [loc]
  (str (:name loc) " lat:" (:lat loc) " - lon:" (:lon loc) "  \n"))

(deftemplate map-page "single.html" [{:keys [api-key article locations geocoded]}]
  [:head] (content (conj (iso-8859-1)
                         (first ((snippet "snippets.html" [:link] [])))
                         (first (google-map-script api-key))
                         (first (construct-map geocoded))))
  [:ul#top-menu :li] (content (top-menu))
  [:body] (set-attr :onload "initialize()")
  [:h1]  (content "View article")
  [:div#article :p] (content article)
;  [:div#article :p]  (content (map str-loc geocoded))
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
