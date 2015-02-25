(ns hubfolio.user-data
  (:require [taoensso.carmine :as car :refer [wcar]]))

(defn save [config username value]
  (let [namespaced-key (str "hubfolio:profile:" username)]
    (car/wcar config (car/set namespaced-key value))
    value))

(defn load [config username]
  (let [namespaced-key (str "hubfolio:profile:" username)]
    (let [value (car/wcar config (car/get namespaced-key))]
      value)))
