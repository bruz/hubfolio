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
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/1.2.0/semantic.min.js"}]
    [:link {:href "/hubfolio.css" :rel "stylesheet"}]]
   [:body
    content]))

(defn home []
  (with-layout
    [:div.ui.search
     [:form {:action "" :method "get"}
      [:div.ui.icon.input
       [:input.prompt {:type "text"
                       :name "username"
                       :placeholder "GitHub username"}]
       [:i.search.icon]]]]))

(defn format-imprecise [number]
  (format "%.1f" (float number)))

(defn user [username stats-conn]
  (let [user (stats/user stats-conn username)
        repos (stats/repos stats-conn username)]
    (with-layout
      [:div
       [:div.ui.segment
        [:div.ui.stackable.very.relaxed.two.column.page.grid
         [:div.row
          [:div.centered.eight.wide.left.aligned.column
           [:div.ui.divided.items
            [:div.link.item
             [:div.ui.small.image
              [:img.ui.rounded.image {:src (user :avatar_url)}]]
             [:div.middle.aligned.content
              [:div.header (str (user :name) " (" (user :login) ")")]
              [:div.description (user :location)]]]]]]]]
       [:div.ui.left.aligned.vertical.segment
        [:div.ui.page.grid
         [:div.row
          [:div.column
        [:div.ui.cards
         (for [repo repos]
          [:div.ui.card
           [:div.content
            [:div.right.floated
             [:div.ui.top.right.attached.green.label "Score"
              [:div.detail (format-imprecise (repo :score))]]]
            [:a.header {:href (repo :html_url)}
             (when-not (= username (get-in repo [:owner :login]))
               [:i.fork.icon])
             (repo :name)]
            [:div.description (repo :description)]]
           [:div.extra.content
            [:div.ui.padded.grid
             [:div.two.column.row
            [:div.ui.label "Starshare"
             [:div.detail
              [:i.star.icon]
              (format-imprecise (repo :starshare))]]
            [:div.right.floated
             [:div.ui.label "User commits"
              [:div.detail
               [:i.user.icon]
               (repo :user-commits)]]]]
             [:div.two.column.row
            [:div.ui.label "Years stale"
             [:div.detail
              [:i.wait.icon]
              (repo :stale-years)]]
            [:div.right.floated
             [:div.ui.label "Total commits"
              [:div.detail
               [:i.users.icon]
               (repo :total-commits)]]]
            ]]]])]]]]]])))

(defn create-routes [stats-conn]
  (routes
    (GET "/" [username]
         (if username (str username)
           (home)))
    (GET "/user/:username" [username] (user username stats-conn))
    (route/files "/" {:root "public"})))

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
