(ns hubfolio.generator
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async :refer [go-loop chan <!]]
            [hubfolio.user-status :as user-status]
            [hubfolio.statistics :as stats]
            [clj-time.format :as f]
            [clj-time.core :as t]))

(defn timestamp []
  (f/unparse (f/formatters :ordinal-date-time) (t/now)))

(defn generator [conn store-config]
  (let [generator-chan (chan)]
    (go-loop []
             (let [username (<! generator-chan)]
               (println (str "Generating stats: " username ", " conn))
               (stats/user conn username)
               (stats/repos conn username)
               (user-status/set-last-updated store-config username (timestamp))))
    generator-chan))

(defrecord Generator [stats-conn store-config]
  component/Lifecycle

  (start [component]
           (assoc component
             :chan (generator stats-conn store-config)))
  (stop [component]
        (assoc component
          :chan nil)))

(defn new-generator [store-config]
  (map->Generator {:store-config store-config}))
