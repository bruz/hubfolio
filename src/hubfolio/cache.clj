(ns hubfolio.cache
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]))

(def one-day (* 24 60 60))
(def one-week (* 7 24 60 60))

(defn get [config key]
  (let [namespaced-key (str "hubfolio:" key)]
    (car/wcar config (car/get namespaced-key))))

(defn set [config key value]
  (let [namespaced-key (str "hubfolio:" key)]
    (car/wcar config (car/setex namespaced-key one-day value))
    value))

(defmacro cache [config key & body]
  `(let [stored-value# (get ~config ~key)]
     (if stored-value#
       stored-value#
       (let [value# ~@body]
         (if (nil? value#)
           value#
           (set ~config ~key value#))))))
