(ns hubfolio.statistics
  (:require [com.stuartsierra.component :as component]
            [hubfolio.github :as github]
            [hubfolio.cache :refer [cache]]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time-core]
            [clojure.math.numeric-tower :as math]))

(defn safe-log2 [n]
  (/ (Math/log (+ n 1)) (Math/log 2)))

(defn repo-total-commits [conn repo]
  (let [contributors (github/repo-contributors conn repo)]
    (reduce + (map :total (vals contributors)))))

(defn repo-user-commits [conn repo username]
  (let [contributors (github/repo-contributors conn repo)]
    (get-in contributors [username :total] 0)))

(defn starshare [conn repo username]
  (let [stars (repo :stargazers_count)
        user-commits (repo-user-commits conn repo username)
        total-commits (repo-total-commits conn repo)]
    (if (zero? total-commits)
      0
      (-> user-commits (/ total-commits) (* stars)))))

(defn years-since [past-time]
  (time-core/in-years
   (time-core/interval (time-coerce/from-string past-time)
                       (time-core/now))))

(defn stale-years [conn repo]
  (years-since (repo :pushed_at)))

(defn score [conn repo username]
  (let [starshare (starshare conn repo username)
        user-commits (repo-user-commits conn repo username)
        total-commits (repo-total-commits conn repo)
        years (stale-years conn repo)]
    (-> (safe-log2 starshare) (* (safe-log2 user-commits)) (/ (math/expt 2 years)))))

(defn source-repo [conn fork-lite username]
  (when (fork-lite :fork)
    (let [fork-owner (get-in fork-lite [:owner :login])
          fork-name (fork-lite :name)
          fork-full (github/user-repo conn fork-owner fork-name)
          source (fork-full :source)
          owner (get-in source [:owner :login])
          contributors (github/repo-contributors conn source)]
      (when (contributors username)
        source))))

(defn source-or-fork [conn repo username]
  (if-let [source (source-repo conn repo username)]
    (if (> (repo :stargazers_count) (source :stargazers_count))
      repo
      (assoc source :fork true))
    repo))

(defn extend-repo-stats [conn repo username]
  (assoc repo
    :starshare (starshare conn repo username)
    :user-commits (repo-user-commits conn repo username)
    :total-commits (repo-total-commits conn repo)
    :stale-years (stale-years conn repo)
    :score (score conn repo username)))

(defn org-repos [conn username]
  (let [user-orgs (github/user-orgs conn username)]
    (->> user-orgs
         (map #(github/org-repos conn (% :login)))
         flatten
         (map #(assoc % :org-repo true)))))

(defn repos [conn username]
  (->> (github/user-repos conn username)
       (concat (org-repos conn username))
       (remove nil?)
       (map #(source-or-fork conn % username))
       (map #(extend-repo-stats conn % username))
       (filter #(not (zero? (% :user-commits))))
       (sort-by :score)
       reverse))

(defn user [conn username]
  (github/user conn username))

(defrecord Statistics [github-auth cache-config]
  component/Lifecycle

  (start [component]
         (let [in-memory-cache (atom {})]
           (assoc component
             :github-auth github-auth
             :cache-config (assoc cache-config :in-memory-cache in-memory-cache))))
  (stop [component]
        ;; no-op
        (assoc component
          :github-auth nil
          :cache-config nil)))

(defn new-statistics [github-auth cache-config]
  (map->Statistics {:github-auth github-auth :cache-config cache-config}))
