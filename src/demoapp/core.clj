(ns demoapp.core
  (:use [demoapp controller]
        [ring.adapter.jetty]
        [ring.middleware resource reload file]
        [ring.util.response]
        [net.cgrand.moustache])
  (:require [clj-egsiona.core :as e])
  (:import [java.net URL]
           [de.l3s.boilerpipe.extractors ArticleExtractor DefaultExtractor]
           ))

(defn in? [element seq]
  (if (some #{element} seq) true false))

(def whitelisted-letters
  #{\A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T \U \V \W \X \Y \Z
    \a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z
    \ª \µ \º \À \Á \Â \Ã \Ä \Å \Æ \Ç \È \É \Ê \Ë \Ì \Í \Î \Ï \Ð \Ñ \Ò \Ó \Ô \Õ \Ö
    \Ø \Ù \Ú \Û \Ü \Ý \Þ \ß \à \á \â \ã \ä \å \æ \ç \è \é \ê \ë \ì \í \î \ï \ð \ñ
    \ò \ó \ô \õ \ö \ø \ù \ú \û \ü \ý \þ \ÿ \space \0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \.
    \: \newline \! \? \= \- \, \{ \} \[ \] \( \) \_ \\ \/ \; \' \@ \# \$ \% \& \*
    \| })

(defn stop-chars [url]
  (distinct (filter #(not (in? % whitelisted-letters)) (extract-content url))))

(defn clean-text [s]
  (->> (filter #(in? % whitelisted-letters) s)
       reverse
       (drop-while #(= % \newline))
       reverse
       (apply str)))

(defn extract-content [url]
  (.. DefaultExtractor INSTANCE (getText (URL. url))))

(comment (defn extract-content [url]
   (.. ArticleExtractor INSTANCE (getText (URL. url)))))

(defn process-url [url]
  (-> url
      extract-content
      clean-text
      e/process-text))

(def p1 "http://www.bt.no/nyheter/innenriks/Spar-sterk-innvandrervekst-i-byene-2669852.html")
(def p2 "http://www.bt.no/nyheter/lokalt/Venter-pa-godkjenning-2669298.html")

(def api-key "AIzaSyA0IrRqPrqcuQTdbUS5o57-EsPEbFKRsOc")

(defn page [req]
  (response "This is page"))

(def routes
  (app
   [""] page
   ["test"] {:get page}
   ["map" &] (delegate map-page)))

(defonce server (run-jetty #'routes {:port 8081 :join? false}))

(e/set-obt "localhost:8085")
;(e/set-obt "/home/ogrim/bin/The-Oslo-Bergen-Tagger")

(comment (e/process-text "Tror du at Sandnes er en lokasjon?"))

(e/set-db {:classname   "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname     "database.db"})


;(e/create-tables)
;(e/process-text "Tror du at Sandnes er en lokasjon?")
