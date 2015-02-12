(ns hubfolio.github
  (:require [tentacles.repos :as repos]
            [tentacles.users :as users]
            [tentacles.orgs :as orgs]
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

(defn user [conn username]
  (let [{:keys [cache-config github-auth]} conn
        key (str "user:" username)]
    (cached users/user cache-config key github-auth username)))

(defn user-repos [conn owner]
  (let [{:keys [cache-config github-auth]} conn
        key (str "repos:" owner)
        options (conj github-auth {:type "public" :all-pages true})]
    (cached repos/user-repos cache-config key options owner)))

(defn user-repo [conn owner repo-name]
  (let [{:keys [cache-config github-auth]} conn
        key (str "repo:" owner ":" repo-name)]
    (cached repos/specific-repo cache-config key github-auth owner repo-name)))

(defn user-orgs [conn user]
  (let [{:keys [cache-config github-auth]} conn
        key (str "user-orgs:" user)]
    (cached orgs/user-orgs cache-config key github-auth user)))

(defn org-repos [conn org]
  (let [{:keys [cache-config github-auth]} conn
        key (str "org-repos:" org)
        options (conj github-auth {:type "all" :all-pages true})]
    (cached repos/org-repos cache-config key options org)))

(defn repo-contributors [conn repo]
  (let [{:keys [cache-config github-auth]} conn
        repo-name (repo :name)
        owner (get-in repo [:owner :login])
        key (str "repo:" owner ":" repo-name ":stats:contributors")
        users (cached repos/contributor-statistics cache-config key github-auth owner repo-name)]
    (zipmap (map #(get-in % [:author :login]) users) users)))

(defn repo-stargazers [conn owner repo-name]
  (let [{:keys [github-auth]} conn
        options (conj github-auth {:all-pages true})]
    (repos/stargazers owner repo-name github-auth)))
