(ns hubfolio.core
  (:require [com.stuartsierra.component :as component]
            [hubfolio.statistics :refer [new-statistics]]
            [hubfolio.generator :refer [new-generator]]
            [hubfolio.server :refer [new-web-server]]
            [hubfolio.web :refer [new-web-handler]]))

(defn system [config]
  (-> (component/system-map
       :statistics (new-statistics (:github config))
       :generator (new-generator (:storage config))
       :web (new-web-handler (:github config) (:storage config))
       :server (new-web-server (-> config :web :port)))
      (component/system-using
        {:generator {:stats-conn :statistics}
         :web {:stats-conn :statistics :generator :generator}
         :server {:web-handler :web}})))

(defn port []
  (or (read-string (System/getenv "PORT"))
      5000))

(defn redis-url []
  (or (System/getenv "REDIS_URL")
      "redis://localhost:6379"))

(defn -main [& args]
  (component/start
   (system {:web
            {:port (port)}
            :storage
            {:uri (redis-url)}
            :github
            {:oauth-token "27e5dcc01e01e5609213a780d10487259667318b"}})))
