(ns demoapp.controller
  (:use [demoapp tools templates]
        [ring.util.response])
  (:require [clj-egsiona.core :as e]
            [clojure.string :as str])
  (:import [java.net URL]
           [de.l3s.boilerpipe.extractors ArticleExtractor DefaultExtractor]))

(defn configure-obt [path db]
  (do (e/set-obt path)
      (e/set-db db)
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
