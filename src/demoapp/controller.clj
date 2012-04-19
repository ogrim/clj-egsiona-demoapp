(ns demoapp.controller
  (:use [demoapp tools templates]
        [ring.util.response])
  (:require [clj-egsiona.core :as e]
            [clojure.string :as str]
            [clojure.java.jdbc :as sql])
  (:import [java.net URL]
           [de.l3s.boilerpipe.extractors ArticleExtractor DefaultExtractor]))

(def ^{:dynamic true :private true} *db* (atom nil))

(defn configure-obt [path db]
  (do (reset! *db* db)
      (e/set-obt path)
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
        numbered-locations (enumerate-locations locations)]
    (->> (result-page (article->html text numbered-locations) numbered-locations)
         response)))
