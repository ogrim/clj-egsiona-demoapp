(ns demoapp.tools)

(defn in? [element seq]
  (if (some #{element} seq) true false))
