(ns demoapp.controller
  (:use [demoapp tools templates]
        [ring.util.response])
  (:require [clj-egsiona.core :as e]
            [clojure.string :as str]
            [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [demoapp.geocode :as geo])
  (:import [java.net URL]
           [de.l3s.boilerpipe.extractors ArticleExtractor DefaultExtractor]))

(def ^{:dynamic true :private true} *google-api* (atom nil))
(def ^{:dynamic true :private true} *db* (atom nil))
(def current-content (atom nil))

(defn create-tables []
  (do (try (sql/with-connection @*db*
          (sql/create-table "articles"
                            [:id :text "PRIMARY KEY"]
                            [:article :text]
                            [:timestamp :datetime]))
        (catch Exception e (println e)))
      (try (sql/with-connection @*db*
          (sql/create-table "locations"
                            [:article_id :text]
                            [:location_id :text]
                            [:name :text]))
        (catch Exception e (println e)))
      (try (sql/with-connection @*db*
          (sql/create-table "tags"
                            [:article_id :text]
                            [:location_id :text]))
        (catch Exception e (println e)))
      (try (sql/with-connection @*db*
        (sql/create-table "geocoded"
                          [:id "INTEGER PRIMARY KEY"]
                          [:name :text]
                          [:latitude :text]
                          [:longitude :text]
                          [:city :text]
                          [:country :text]
                          [:street_name :text]
                          [:street_number :text]
                          [:postal_code :text]
                          [:region :text]))
           (catch Exception e (println e)))))

(defn get-next-id []
  (let [result (atom [])]
    (sql/with-connection @*db*
      (sql/with-query-results rs ["select id from articles order by id"]
        (doseq [row rs] (swap! result conj (-> row :id Integer/parseInt)))))
    (if (empty? @result) 1 (-> @result sort last inc))))

(defn get-location [article-id location-id]
  (-> (sql/with-connection @*db*
        (sql/with-query-results rs
          ["select name from locations where article_id = ? and location_id = ?" article-id location-id]
          (doall rs)))
        first :name))

(defn get-all-article-id []
  (let [result (atom [])]
    (sql/with-connection @*db*
      (sql/with-query-results rs
        ["select id from articles"]
        (doseq [row rs] (swap! result conj row))))
    (->> @result (map :id) (map #(Integer/parseInt %)) sort reverse (into []))))

(defn get-article [id]
  (let [result (atom {})]
    (sql/with-connection @*db*
      (swap! result assoc :article
             (-> (sql/with-query-results rs
                   ["select * from articles where id = ?" id]
                   (doall rs))
                 first :article))
      (swap! result assoc :tags (->> (sql/with-query-results rs
                                       ["select * from tags where article_id = ?" id]
                                       (doall rs))
                                     (map #(get-location (:article_id %) (:location_id %)))
                                     (into []))))
    @result))

(defn listable-article [i]
  (let [{tags :tags text :article} (get-article i)]
    {:tagcount (count tags)
     :preview (str (apply str (take 100 text)) "...")
     :i i}))

(defn persist-article [i text numbered-locations tags]
  (sql/with-connection @*db*
    (sql/insert-rows "articles" [i text (java.sql.Timestamp. (.getTime (java.util.Date.)))])
    (doseq [[num loc] numbered-locations]
      (sql/insert-rows "locations" [i num loc]))
    (doseq [tag tags]
      (sql/insert-rows "tags" [i tag]))))

(defn configure-obt [path db api-key]
  (do (reset! *db* db)
      (reset! *google-api* api-key)
      (create-tables)
      (e/set-obt path)
      (e/set-db db)
      (geo/set-db db)
      (e/create-tables)))

(def whitelisted-letters
  #{\A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T \U \V \W \X \Y \Z
    \a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z
    \ª \µ \º \À \Á \Â \Ã \Ä \Å \Æ \Ç \È \É \Ê \Ë \Ì \Í \Î \Ï \Ð \Ñ \Ò \Ó \Ô \Õ \Ö
    \Ø \Ù \Ú \Û \Ü \Ý \Þ \ß \à \á \â \ã \ä \å \æ \ç \è \é \ê \ë \ì \í \î \ï \ð \ñ
    \ò \ó \ô \õ \ö \ø \ù \ú \û \ü \ý \þ \ÿ \space \0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \.
    \: \newline \! \? \= \- \, \{ \} \[ \] \( \) \_ \\ \/ \; \' \@ \# \$ \% \& \*
    \| })

(defn clean-text [s]
  (->> (filter #(in? % whitelisted-letters) s)
       reverse
       (drop-while #(= % \newline))
       reverse
       (apply str)
       strip-newlines))

(defn extract-content-default [url]
  (.. DefaultExtractor INSTANCE (getText (URL. url))))

(defn extract-content [url]
  (.. ArticleExtractor INSTANCE (getText (URL. url))))

(defn process-url [url]
  (-> url
      extract-content
      clean-text
      e/process-text))

(defn process-text [s]
  (-> s clean-text e/process-text))

(defn process-params [params]
  (if-let [url (get params "url")]
    (let [content (extract-content url)]
      [(clean-text content) (process-text content)])
    (let [content (get params "text")]
      [(clean-text content) (process-text content)])))

(defn enumerate-locations [locations]
  (let [i (atom 0)]
    (map #(vector (swap! i inc) %) locations)))

(defn get-location-i [location locations]
  (loop [[[i name] & more] locations]
    (cond (empty? name) nil
          (= name location) i
          :else (recur more))))

(defn add-span [i s]
  (let [trimmed (-> s .trim trim-trailing-punctuation)
        end (.substring s (count trimmed) (count s))]
    (str "<span class=\"highlight " "tag-" i "\">" trimmed "</span>" end " ")))

(defn compare-multi [v1 v2]
  (= (map #(trim-trailing-punctuation (.toLowerCase %)) v1) v2))

(defn process-multi-location [s [i location]]
  (if (empty? location) s
      (let [text (->> (str/split s #"\s") (filter seq))
            in-span? (atom false)]
        (loop [[word & more :as tokens] text, acc [], result []]
          (cond (empty? word)
                (->> result flatten (map #(str % " ")) (apply str))

                @in-span?
                (if (re-seq #"</span>" word)
                  (do (reset! in-span? false)
                      (recur more [] (conj result word)))
                  (recur more [] (conj result word)))

                (re-seq #"class=\"highlight" word)
                (do (reset! in-span? true)
                    (if (empty? acc)
                      (recur more [] (conj result word))
                      (recur tokens [] (conj result acc))))

                (compare-multi acc location)
                (recur tokens [] (conj result (add-span i (apply str (map #(str % " ") acc)))))

                (and (empty? acc)
                     (= (trim-trailing-punctuation (.toLowerCase word)) (first location)))
                (recur more [word] result)

                (= (trim-trailing-punctuation (.toLowerCase word)) (nth location (count acc)))
                (recur more (conj acc word) result)

                (= (trim-trailing-punctuation (.toLowerCase word)) (first location))
                (recur more [word] (conj result acc))

                (seq acc)
                (recur tokens [] (conj result acc))

                :else (recur more [] (conj result word)))))))

(defn process-single-location [locations s]
  (if (empty? locations) s
      (let [location-lookup (map second locations)
            location-map (into {} (map #(vector (second %) (first %)) locations))
            word-fn #(-> % .trim .toLowerCase trim-trailing-punctuation)]
        (loop [[word & more :as tokens] (->> (str/split s #"\s") (remove empty?))
               in-span? false
               result []]
          (cond (empty? word) result

                in-span?
                (recur more (if (re-seq #"</span>" word) false true) (conj result word))

                (and (re-seq #"<span" word) (re-seq #"class=\"highlight" (first more)))
                (recur more true (conj result word))

                (in? (word-fn word) location-lookup)
                (recur more false (conj result (add-span (get location-map (word-fn word)) word)))

                :else (recur more false (conj result word)))))))

(defn article->html [article locations]
  (if (empty? locations) (str->html article)
      (let [single-locations (filter #(= (count (str/split (second %) #"\s")) 1) locations)
            multi-locations (->> locations
                                 (map #(vector (first %) (str/split (second %) #"\s")))
                                 (filter #(> (count (second %)) 1)))]
        (->> (reduce process-multi-location article multi-locations)
             (process-single-location single-locations)
             (interpose " ")
             (apply str)
             .trim
             str->html))))

(defn view-results-page [req]
  (let [params (:form-params req)
        [text locations] (process-params params)
        numbered-locations (enumerate-locations locations)
        html (article->html text numbered-locations)
        _ (reset! current-content {:id (get-next-id) :text text :locations numbered-locations})]
    (->> (result-page html numbered-locations)
         response)))

(defn view-start-page [req]
  (->> (start-page) response))

(defn view-article [req id]
  (let [{:keys [tags article]} (get-article id)
        locations (enumerate-locations tags)]
    (if (nil? article) (redirect "/article")
        (->> (article-page (article->html article locations) locations) response))))

(defn view-article-list [req]
  (let [articles (map listable-article (get-all-article-id))]
    (->> articles (article-list-page) response)))

(defn post-article [req]
  (let [params (:form-params req)
        tags (->> (filter #(= (second %) "true") params)
                  (map first)
                  (map #(str/split % #"tag-"))
                  (map second))
        _ (persist-article (:id @current-content)
                           (:text @current-content)
                           (:locations @current-content)
                           tags)]
    (redirect (str "/article/" (:id @current-content)))))

(defn geocode [[i location]]
  (let [result (->> (geo/geocode location) seq first)
        country (:country result)]
    {:i (str i)
     :name (.toLowerCase location)
     :lat (-> result :latitude str)
     :lon (-> result :longitude str)
     :country (if (seq country) (str/lower-case country) "")}))

(defn google-view [req id]
  (let [{:keys [tags article]} (get-article id)
        locations (enumerate-locations tags)
        geocode (map geocode locations)]
    (->> (map-page {:api-key @*google-api*
                    :article (article->html article locations)
                    :locations locations
                    :geocoded geocode})
         response)))

(defn- get-geocoded [id]
  (->> id get-article :tags enumerate-locations (map geocode)))
