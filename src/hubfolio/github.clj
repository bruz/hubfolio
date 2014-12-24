(ns hubfolio.github
  (:require [tentacles.repos :as repos]))

(defn user-repos [auth user]
  (repos/user-repos user (conj auth {:type "all" :all-pages true})))
