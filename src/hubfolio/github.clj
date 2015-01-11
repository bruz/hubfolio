(ns hubfolio.github
  (:require [tentacles.repos :as repos]
            [hubfolio.cache :refer [cache]]))

(defn request [resource args]
  (loop [retries 10]
    (let [response (apply resource args)]
      (cond
       (<= retries 0)
       nil

       (= response :tentacles.core/accepted)
       (do
         (println "Waitin for github stats...")
         (Thread/sleep 1000)
         (recur (dec retries)))

       :else
       response))))

(defn cached [resource cache-config key options & xs]
  (let [args (concat xs [options])]
    (cache cache-config key
           (request resource args))))

(defn user-repos [conn owner]
  (let [{:keys [cache-config github-auth]} conn
        key (str "repos:" owner)
        options (conj github-auth {:type "all" :all-pages true})]
    (cached repos/user-repos cache-config key options owner)))

(defn user-repo [conn owner repo-name]
  (let [{:keys [cache-config github-auth]} conn
        key (str "repo:" owner ":" repo-name)]
    (cached repos/specific-repo cache-config key github-auth owner repo-name)))

(defn repo-contributors [conn repo]
  (let [{:keys [cache-config github-auth]} conn
        repo-name (repo :name)
        owner (get-in repo [:owner :login])
        key (str "repo:" owner ":" repo-name ":stats:contributors")
        users (cached repos/contributor-statistics cache-config key github-auth owner repo-name)]
    (zipmap (map #(get-in % [:author :login]) users) users)))
