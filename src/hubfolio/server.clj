(ns hubfolio.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]))

(defrecord WebServer [port server web-handler]
  component/Lifecycle
  (start [component]
    (let [handler (:handler web-handler)
          server (run-jetty handler {:port port :join? false})]
      (assoc component :server server)))
  (stop [component]
    (when server
      (.stop server)
      (assoc component :server nil))))

(defn new-web-server
  [port]
  (map->WebServer {:port port}))
