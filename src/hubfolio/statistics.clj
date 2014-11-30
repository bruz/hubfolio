(ns hubfolio.statistics
  (:require [com.stuartsierra.component :as component]
            [hubfolio.github :refer [user-repos]]))

(defn github [resource github-auth]
  (resource github-auth))

(defn repo-url [github-auth]
  (let [response (github user-repos github-auth)]
    (-> response first :url)))

(defrecord Statistics [github-auth cache-config]
  component/Lifecycle

  (start [component]
    (assoc component :statistics {:github-auth github-auth}))
  (stop [component]
    ;; no-op
    (assoc component :statistics nil)))

(defn new-statistics [github-auth cache-config]
  (map->Statistics {:github-auth github-auth :cache-config cache-config}))
