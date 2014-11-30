(ns hubfolio.github
  (:require [tentacles.repos :as repos]))

(defn user-repos [auth]
  (repos/user-repos "bruz" auth))
