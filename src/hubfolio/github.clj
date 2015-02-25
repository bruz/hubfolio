(ns hubfolio.github
  (:require [tentacles.core :as core]
            [tentacles.orgs :as orgs]
            [tentacles.repos :as repos]
            [tentacles.users :as users]
            [hubfolio.cache :refer [cache delete]]))

(defn request [resource & args]
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

(defn user [conn username]
  (let [{:keys [github-auth]} conn
        key (str "user:" username)]
    (request users/user username github-auth)))

(defn user-repos [conn owner]
  (let [{:keys [github-auth]} conn
        key (str "repos:" owner)
        options (conj github-auth {:type "public" :all-pages true})]
    (request repos/user-repos owner github-auth)))

(defn user-repo [conn owner repo-name]
  (let [{:keys [github-auth]} conn
        key (str "repo:" owner ":" repo-name)]
    (request repos/specific-repo owner repo-name github-auth)))

(defn user-orgs [conn user]
  (let [{:keys [github-auth]} conn
        key (str "user-orgs:" user)]
    (request orgs/user-orgs user github-auth)))

(defn org-repos [conn org]
  (let [{:keys [github-auth]} conn
        key (str "org-repos:" org)
        options (conj github-auth {:type "all" :all-pages true})]
    (request repos/org-repos org options)))

(defn repo-contributors [conn repo]
  (let [{:keys [github-auth]} conn
        repo-name (repo :name)
        owner (get-in repo [:owner :login])
        key (str "repo:" owner ":" repo-name ":stats:contributors")
        users (request repos/contributor-statistics owner repo-name github-auth)]
    (zipmap (map #(get-in % [:author :login]) users) users)))

(defn repo-stargazers [conn owner repo-name]
  (let [{:keys [github-auth]} conn
        options (conj github-auth {:all-pages true})]
    (request repos/stargazers owner repo-name github-auth)))

(defn rate-limit [conn]
  (let [{:keys [github-auth]} conn]
    (request core/rate-limit github-auth)))
