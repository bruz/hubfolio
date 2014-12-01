(ns hubfolio.cache
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]))

(defn get [config key]
  (car/wcar config (car/get key)))

(defn set [config key value]
  (car/wcar config (car/set key value))
  value)

(defmacro cache [config key & body]
  `(let [stored-value# (get ~config ~key)]
     (if stored-value#
       stored-value#
       (set ~config ~key ~@body))))
