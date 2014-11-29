(ns hubfolio.web
  (:require [compojure.core :refer [routes GET]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [com.stuartsierra.component :as component]
            [hubfolio.statistics :refer [repo-url]]))

(defn statistic [stat stats-conn]
  (stat stats-conn))

(defn create-routes [stats-conn]
  (routes
    (GET "/" [] (repo-url stats-conn))))

(defrecord WebHandler [stats-conn]
  component/Lifecycle

  (start [component]
    (let [handler (wrap-defaults (create-routes stats-conn) site-defaults)]
      (assoc component :handler handler)))
  (stop [component]
    ;; no-op
    (assoc component :handler nil)))

(defn new-web-handler []
  (map->WebHandler {:stats-conn nil}))
