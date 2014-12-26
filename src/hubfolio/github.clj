(ns hubfolio.github
  (:require [tentacles.repos :as repos]
            [hubfolio.cache :refer [cache]]))

(defn request [resource args]
  (let [response (apply resource args)]
    (println "call-remaining: " (-> response meta :api-meta :call-remaining))
    (println "etag: " (-> response meta :api-meta :etag))
    response))

(defn cached [resource cache-config key options & xs]
  (let [args (concat xs [options])]
    (cache cache-config key
           (request resource args))))

(defn user-repos [conn username]
  (let [{:keys [cache-config github-auth]} conn
        key (str "repos:" username)
        options (conj github-auth {:type "all" :all-pages true})]
    (cached repos/user-repos cache-config key options username)))
