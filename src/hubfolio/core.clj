(ns hubfolio.core
  (:require [com.stuartsierra.component :as component]
            [hubfolio.statistics :refer [new-statistics]]
            [hubfolio.generator :refer [new-generator]]
            [hubfolio.server :refer [new-web-server]]
            [hubfolio.web :refer [new-web-handler]]
            [environ.core :refer [env]])
  (:gen-class))

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

(defn -main [& args]
  (component/start
   (system {:web
            {:port (env :port)}
            :storage
            {:spec {:uri (env :redis-url)}}
            :github
            {:oauth-token (env :oauth-token)}})))
