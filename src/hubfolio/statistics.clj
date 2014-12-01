(ns hubfolio.statistics
  (:require [com.stuartsierra.component :as component]
            [hubfolio.github :refer [user-repos]]
            [hubfolio.cache :refer [cache]]))

(defn github [resource github-auth]
  (resource github-auth))

(defn repo-url [stats-conn]
  (let [{:keys [cache-config github-auth]} stats-conn]
    (cache cache-config "repo-url"
      (let [response (github user-repos github-auth)]
        (-> response first :url)))))

(defrecord Statistics [github-auth cache-config])

(defn new-statistics [github-auth cache-config]
  (map->Statistics {:github-auth github-auth :cache-config cache-config}))
