(ns hubfolio.cache
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]))

(defn set-memory [config key value]
  (swap! (config :in-memory-cache) assoc key value))

(defn set [config key value]
  (let [namespaced-key (str "hubfolio:" key)]
    (set-memory config key value)
    (car/wcar config (car/set namespaced-key value))
    value))

(defn get-memory [config key]
  (@(config :in-memory-cache) key))

(defn get [config key]
  (let [namespaced-key (str "hubfolio:" key)]
    (or
     (get-memory config key)
     (let [value (car/wcar config (car/get namespaced-key))]
       (set-memory config key value)
       value))))

(defmacro cache [config key & body]
  `(let [stored-value# (get ~config ~key)]
     (if stored-value#
       stored-value#
       (let [value# ~@body]
         (if (nil? value#)
           value#
           (set ~config ~key value#))))))
