(ns hubfolio.user-status
  (:require [hubfolio.github :as github]
            [hubfolio.statistics :as stats]
            [clojure.core.async :as async :refer [go >!]]
            [taoensso.carmine :as car :refer [wcar]]))

(def hash-name "hubfolio:last-updated")

(defn check-starred [generator github-auth username]
  (let [stargazers (github/repo-stargazers github-auth "bruz" "hubfolio")
        generator-chan (:chan generator)]
    (if (some #(= (:login %) username) stargazers)
      (do
        (go (>! generator-chan username))
        :generating)
      :not-opted-in)))

(defn check-opted-in [generator github-auth username]
  (let [response (stats/user {:github-auth github-auth} username)]
    (if (= (:status response) 404)
      :no-user
      (check-starred generator github-auth username))))

(defn set-last-updated [store-config username last-updated]
  (car/wcar store-config (car/hset hash-name username last-updated))
  last-updated)

(defn all-last-updated [store-config]
  (->> (car/hgetall hash-name)
       (car/wcar store-config)
       (partition 2)))

(defn get-last-updated [generator github-auth store-config username]
  (let [last-updated (->> (car/hget hash-name username)
                          (car/wcar store-config)
                          (keyword))]
    (->> (case (or last-updated :not-opted-in)
           :generating :generating
           :failed :failed
           :not-opted-in (check-opted-in generator github-auth username)
           last-updated)
         (set-last-updated store-config username))))

(defn get-status [generator github-auth store-config username]
  (let [last-updated (get-last-updated generator github-auth store-config username)]
    (if (some #{last-updated} [:generating :failed :not-opted-in :no-user])
      last-updated
      :generated)))
