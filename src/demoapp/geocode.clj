(ns demoapp.geocode
  (:require [geocoder.core :as g]
;            [clj-time.core :as t]
            [clojure.java.jdbc :as sql]))

(def ^{:dynamic true :private true} *reset-time* (atom nil))
(def ^{:dynamic true :private true} *api-calls* (atom 0))
(def ^{:private true} api-max-requests 2500)
(def ^{:dynamic true}  *db* (atom nil))

(defn set-db [db-spec]
  (reset! *db* db-spec))

(comment (defn- reset-time []
   (reset! *reset-time* (t/plus (t/now) (t/hours 24)))))

(defn- can-call-api? [] true)

(comment (defn- can-call-api? []
           (cond (not @*reset-time*) (do (reset-time) true)
                 (< @*api-calls* api-max-requests) true
                 (t/after? (t/now) @*reset-time*) (do (reset-time) (reset! *api-calls* 0) true)
                 :else false)))

(defn- db-locations [name]
  (if (nil? @*db*) nil
      (->> (sql/with-connection @*db*
             (sql/with-query-results rs ["SELECT * FROM geocoded WHERE name = ?"
                                         (.toLowerCase name)]
               (doall rs)))
           (sort-by :id))))

(defn- persist-geocoded [location]
  (cond (nil? @*db*) false
        :else (do (sql/with-connection @*db*
                    (sql/insert-rows "geocoded" location))
                  true)))

(try (sql/with-connection @*db*
          (sql/create-table "articles"
                            [:id :text "PRIMARY KEY"]
                            [:article :text]
                            [:timestamp :datetime]))
        (catch Exception e (println e)))

(defn geocode [address]
  (let [location (db-locations address)]
    (if (empty? location)
      (let [geocoded (try (g/geocode-address address) (catch Exception _ nil))]
        (if (not (nil? geocoded))
          (doall (map #(persist-geocoded [nil
                                          (.toLowerCase address)
                                          (:latitude (:location %))
                                          (:longitude (:location %))
                                          (:city %)
                                          (:name (:country %))
                                          (:street-name %)
                                          (:street-number %)
                                          (:postal-code %)
                                          (:region %)])
                      geocoded)))
        geocoded)
      location)))
