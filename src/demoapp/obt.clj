(ns demoapp.obt
  (:use [demoapp tools])
  (:require [clj-egsiona.core :as e])
  (:import [java.net URL]
           [de.l3s.boilerpipe.extractors ArticleExtractor DefaultExtractor]))

(defn configure-obt [path db]
  (do (e/set-obt path)
      (e/set-db db)
      (e/create-tables)))

;(e/set-obt "localhost:8085")
;(e/set-obt "/home/ogrim/bin/The-Oslo-Bergen-Tagger")
;(e/create-tables)
;(e/set-db {:classname   "org.sqlite.JDBC"
;           :subprotocol "sqlite"
;           :subname     "database.db"})

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
