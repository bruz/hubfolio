(ns hubfolio.statistics
  (:require [com.stuartsierra.component :as component]
            [hubfolio.github :as github]
            [hubfolio.cache :refer [cache]]))

(defn starshare [conn owner repo-name username]
  (let [repo (github/user-repo conn owner repo-name)
        stars (repo :stargazers_count)
        contributors (github/repo-contributors conn owner repo-name)
        user-commits ((contributors username) :total)
        total-commits (reduce + (map #(% :total) (vals contributors)))]
    (-> user-commits (/ total-commits) (* stars))))

(defn repos [owner conn]
  (github/user-repos conn owner))

(defrecord Statistics [github-auth cache-config])

(defn new-statistics [github-auth cache-config]
  (map->Statistics {:github-auth github-auth :cache-config cache-config}))
