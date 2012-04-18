(ns demoapp.tools
  (:require [clojure.string :as str]))

(defn in? [element seq]
  (if (some #{element} seq) true false))

(defn trim-trailing-punctuation [s]
  (first (str/split s #"[.,:;!?]+\Z")))

(defn punctuation? [c]
  (in? c #{\. \, \: \; \! \? }))

(defn strip-newlines [s]
  (->> s (map #(if (= % \newline) " " %)) (apply str)))
