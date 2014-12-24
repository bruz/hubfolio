(ns hubfolio.statistics
  (:require [com.stuartsierra.component :as component]
            [hubfolio.github :as github]
            [hubfolio.cache :refer [cache]]))

(defn github [resource github-auth & xs]
  (let [response (apply resource github-auth xs)]
    (println "call-remaining: " (-> response meta :api-meta :call-remaining))
    response))

(defn cached [resource conn key & xs]
  (let [{:keys [cache-config github-auth]} conn]
    (cache cache-config key
      (apply github resource github-auth xs))))

(defn repos [username conn]
  (let [key (str "repos:" username)]
    (cached github/user-repos conn key username)))

(defrecord Statistics [github-auth cache-config])

(defn new-statistics [github-auth cache-config]
  (map->Statistics {:github-auth github-auth :cache-config cache-config}))
