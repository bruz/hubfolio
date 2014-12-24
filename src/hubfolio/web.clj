(ns hubfolio.web
  (:require [compojure.core :refer [routes GET]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [com.stuartsierra.component :as component]
            [hubfolio.statistics :as stats]
            [hiccup.core :refer [html]]))

(defn with-layout [content]
  (html
   [:head
    [:link {:href "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/1.2.0/semantic.min.css" :rel "stylesheet"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/1.2.0/semantic.min.js"}]]
   [:body
    [:div.ui.center.aligned.segment
     [:h1 "Hubfolio"] content]]))

(defn home []
  (with-layout
    [:div.ui.search
     [:form {:action "" :method "get"}
      [:div.ui.icon.input
       [:input.prompt {:type "text"
                       :name "username"
                       :placeholder "GitHub username"}]
       [:i.search.icon]]]]))

(defn user [username stats-conn]
  (let [repos (stats/repos username stats-conn)]
    (with-layout
      [:ul
       (for [repo repos]
         [:li (repo :name)])])))

(defn create-routes [stats-conn]
  (routes
    (GET "/" [username]
         (if username (str username)
           (home)))
    (GET "/user/:username" [username] (user username stats-conn))))

(defrecord WebHandler [stats-conn]
  component/Lifecycle

  (start [component]
    (let [handler (wrap-defaults (create-routes stats-conn) site-defaults)]
      (assoc component :handler handler)))
  (stop [component]
    ;; no-op
    (assoc component :handler nil)))

(defn new-web-handler []
  (map->WebHandler {}))
