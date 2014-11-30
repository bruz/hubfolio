(ns hubfolio.core
  (:require [com.stuartsierra.component :as component]
            [hubfolio.statistics :refer [new-statistics]]
            [hubfolio.server :refer [new-web-server]]
            [hubfolio.web :refer [new-web-handler]]))

(defn system [config]
  (-> (component/system-map
       :statistics (new-statistics (:github config) (:cache config))
       :web (new-web-handler)
       :server (new-web-server (-> config :web :port)))
      (component/system-using
        {:web {:stats-conn :statistics}
         :server {:web-handler :web}})))
