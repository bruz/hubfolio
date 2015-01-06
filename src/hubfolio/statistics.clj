(ns hubfolio.statistics
  (:require [com.stuartsierra.component :as component]
            [hubfolio.github :as github]
            [hubfolio.cache :refer [cache]]))

(defn starshare [conn owner repo-name username]
  (let [repo (github/user-repo conn owner repo-name)
        stars (repo :stargazers_count)
        contributors (github/repo-contributors conn owner repo-name)
        user-contributions (contributors username)
        user-commits (if user-contributions (user-contributions :total) 0)
        total-commits (reduce + (map #(% :total) (vals contributors)))]
    (if (= 0 total-commits)
      0
      (-> user-commits (/ total-commits) (* stars)))))

(defn source-repo [conn fork-lite username]
  (if (fork-lite :fork)
    (let [fork-owner (get-in fork-lite [:owner :login])
          fork-name (fork-lite :name)
          fork-full (github/user-repo conn fork-owner fork-name)
          source (fork-full :source)
          owner (get-in source [:owner :login])
          repo-name (source :name)
          contributors (github/repo-contributors conn owner repo-name)]
      (if (contributors username)
        source
        nil))
    nil))

(defn repos [conn username]
  (let [owned-repos (github/user-repos conn username)
        source-repos (map #(source-repo conn % username) owned-repos)]
    (->> owned-repos
         (concat source-repos)
         (remove nil?))))

(defrecord Statistics [github-auth cache-config])

(defn new-statistics [github-auth cache-config]
  (map->Statistics {:github-auth github-auth :cache-config cache-config}))
