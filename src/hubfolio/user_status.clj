(ns hubfolio.user-status
  (:require [hubfolio.github :as github]
            [clojure.core.async :as async :refer [go >!]]
            [taoensso.carmine :as car :refer [wcar]]))

(def hash-name "hubfolio:last-updated")

(defn check-starred [generator github-auth store-config username]
  (let [stargazers (github/repo-stargazers github-auth "bruz" "hubfolio")
        generator-chan (:chan generator)]
    (if true;;(some #(= (:login %) username) stargazers)
      (do
        (go (>! generator-chan username))
        :generating)
      :not-opted-in)))

(defn set [store-config username last-updated]
  (car/wcar store-config (car/hset hash-name username last-updated))
  last-updated)

(defn get [generator github-auth store-config username]
  (let [last-updated (->> (car/hget hash-name username)
                          (car/wcar store-config)
                          (keyword))]
    (->> (case (or last-updated :not-opted-in)
           :generating :generating
           :not-opted-in (check-starred generator github-auth store-config username)
           nil (check-starred generator github-auth store-config username)
           :generated)
         (set store-config username))))
