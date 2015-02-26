(ns hubfolio.web
  (:require [compojure.core :refer [routes GET POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [redirect]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [com.stuartsierra.component :as component]
            [hubfolio.statistics :as stats]
            [hubfolio.user-status :as user-status]
            [hubfolio.user-data :as user-data]
            [hiccup.core :refer [html]]))

(defn with-layout [content]
  (html
   [:head
    [:link {:href "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/1.2.0/semantic.min.css" :rel "stylesheet"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/1.2.0/semantic.min.js"}]
    [:link {:href "/hubfolio.css" :rel "stylesheet"}]
    [:script {:src "/hubfolio.js"}]]
   [:body
    content]))

(defn home []
  (with-layout
    [:div
     [:div.ui.segment
      [:div.ui.center.aligned.stackable.very.relaxed.page.grid
       [:div.row
        [:div.fourteen.wide.column
         [:h1.ui.header "Hubfolio"]
         [:div.ui.search
          [:form {:action "/" :method "post"}
           (anti-forgery-field)
           [:div.ui.icon.input
            [:input.prompt {:type "text"
                            :name "username"
                            :placeholder "GitHub username"}]
            [:i.search.icon]]]]]]]]]))

(defn format-imprecise [number]
  (format "%.1f" (float number)))

(defn not-opted-in [username]
  (with-layout
    [:div.ui.stackable.segment
     [:div.ui.stackable.center.aligned.page.grid
      [:div.fourteen.wide.column
       [:h1.ui.header (str username " has not opted in")]
       [:p (str "Are you " username "? Have your hubfolio made by ")
        [:a {:href "https://github.com/bruz/hubfolio"} "starring the hubfolio repo."]]
       [:a.ui.labeled.primary.button.check-status "Check again"]]]]))

(defn generating [username]
  (with-layout
    [:div
     [:div.ui.segment
      [:div.ui.active.inverted.dimmer
       [:div.ui.indeterminate.text.active.loader "Generating... this may take a while"]]
      [:br]
      [:br]
      [:br]
      [:br]
      [:br]]
     [:script "$(document).ready(function(){ checkStatus(); });"]]))

(defn generated [username store-config]
  (let [{:keys [user repos]} (user-data/retrieve store-config username)]
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
             [:div.ui.top.right.attached.green.label {:data-content "Combination of starshare, user commits and years stale into a single metric"} "Score"
              [:div.detail (format-imprecise (repo :score))]]]
            [:a.header {:href (repo :html_url)}
             (cond
              (repo :fork) [:i.fork.icon]
              (repo :org-repo) [:i.users.icon])
             (repo :name)]
            [:div.description (repo :description)]]
           [:div.extra.content
            [:div.ui.padded.grid
             [:div.two.column.row
            [:div.ui.small.label {:data-content (str "Number of stars " username " gets credit for on this repo. Determined by applying the percentage of commits the user has contributed to the total number of stars.")} "Starshare"
             [:div.detail
              [:i.star.icon]
              (format-imprecise (repo :starshare))]]
            [:div.right.floated
             [:div.ui.small.label {:data-content (str "Commits by " username " to this repo")} "User commits"
              [:div.detail
               [:i.user.icon]
               (repo :user-commits)]]]]
             [:div.two.column.row
            [:div.ui.small.label {:data-content "Number of years since the last commit by anyone to this repo"} "Years stale"
             [:div.detail
              [:i.wait.icon]
              (repo :stale-years)]]
            [:div.right.floated
             [:div.ui.small.label {:data-content "Total commits by all users to this repo"} "Total commits"
              [:div.detail
               [:i.users.icon]
               (repo :total-commits)]]]]]]])]]]]]])))

(defn user [username stats-conn generator github-auth store-config]
  (let [status (user-status/get-status generator github-auth store-config username)]
    (case
      status
      :not-opted-in (not-opted-in username)
      :generating (generating username)
      :generated (generated username store-config))))

(defn create-routes [stats-conn generator github-auth store-config]
  (routes
    (GET "/" [] (home))
    (route/files "/" {:root "public"})
    (POST "/" [username] (redirect (str "/" username)))
    (GET "/:username" [username] (user username stats-conn generator github-auth store-config))
    (GET "/:username/status" [username] (name (user-status/get-status generator github-auth store-config username)))))

(defrecord WebHandler [stats-conn generator github-auth store-config]
  component/Lifecycle

  (start [component]
    (let [handler (wrap-defaults (create-routes stats-conn generator github-auth store-config) site-defaults)]
      (assoc component
        :handler handler
        :github-auth github-auth
        :store-config store-config)))
  (stop [component]
    ;; no-op
    (assoc component
      :handler nil
      :github-auth nil
      :store-config nil)))

(defn new-web-handler [github-auth store-config]
  (map->WebHandler {:github-auth github-auth :store-config store-config}))
