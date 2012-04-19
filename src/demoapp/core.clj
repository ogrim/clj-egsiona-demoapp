(ns demoapp.core
  (:use [demoapp tools templates]
        [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response]
        [net.cgrand.moustache])
  (:require [demoapp.obt :as obt]
            [clojure.string :as str]))

(def api-key "AIzaSyA0IrRqPrqcuQTdbUS5o57-EsPEbFKRsOc")

(obt/configure-obt "localhost:8085"
                   {:classname   "org.sqlite.JDBC"
                    :subprotocol "sqlite"
                    :subname     "database.db"})

(defn process-params [params]
  (if-let [url (get params "url")]
    (let [content (obt/extract-content url)]
      [(obt/clean-text content) (obt/process-text content)])
    (let [content (get params "text")]
      [(obt/clean-text content) (obt/process-text content)])))

(defn add-span [s]
  (let [trimmed (trim-trailing-punctuation s)
        end (.substring s (count trimmed) (count s))]
    (str "<span class=\"highlight " "tag-" (.toLowerCase trimmed) "\">" trimmed "</span>" end " ")))

(defn compare-multi [v1 v2]
  (= (map #(trim-trailing-punctuation (.toLowerCase %)) v1) v2))

(defn process-multi-location [s location]
  (if (empty? location) s
   (let [text (->> (str/split s #"\s") (filter seq))]
     (loop [[word & more :as tokens] text, acc [], result []]
       (cond (empty? word)
             (->> result flatten (map #(str % " ")) (apply str))

             (compare-multi acc location)
             (recur tokens [] (conj result (add-span (apply str (map #(str % " ") acc)))))

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

(defn article->html [article locations]
  (let [tokens (str/split article #"\s")
        single-locations (filter #(= (count (str/split % #"\s")) 1) locations)
        multi-locations (->> locations
                             (map #(str/split % #"\s"))
                             (filter #(> (count %) 1)))
        word-fn (fn [s] (-> s .toLowerCase trim-trailing-punctuation))
        test-fn (fn [s] (in? (word-fn s) single-locations))
        single-span (map #(if (test-fn %) (add-span %) %) tokens)
        multi-span (->> single-span (interpose " ") (apply str))]
    (->> (reduce process-multi-location multi-span multi-locations)
         .trim
         str->html)))

(defn view-results-page [req]
  (let [params (:form-params req)
        [text locations] (process-params params)]
    (->> (result-page (article->html text locations) locations)
         response)))

(def routes
  (app
   (wrap-file "resources")
   ["" &] (fn [_] (->> (start-page) response))
   ["url" &] {:get (fn [_] (->> (input-page) response))
              :post (wrap-params view-results-page)}
   ["text" &] {:get (fn [_] (->> (text-input-page) response))
               :post (wrap-params view-results-page)}
   ["map" &] (delegate map-page)
   ["zomg" &] (fn [_] (->> (zomg-page) response))))

(defonce server (run-jetty #'routes {:port 8081 :join? false}))
