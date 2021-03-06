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
    [:link {:href "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/1.10.2/semantic.min.css" :rel "stylesheet"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/semantic-ui/1.10.2/semantic.min.js"}]
    [:script {:src "https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS_CHTML"}]
    [:link {:href "/hubfolio.css" :rel "stylesheet"}]
    [:script {:src "/hubfolio.js"}]]
   [:body
    [:nav.ui.fixed.menu
     [:a.brand.item {:href "/"} "Hubfolio"]
     [:a.brand.item {:href "/faq"} "FAQ"]
     [:div.right.menu
      [:div.item
       [:div.ui.transparent.icon.input
         [:form {:action "/" :method "post"}
          (anti-forgery-field)
          [:input.prompt {:type "text"
                           :name "username"
                           :placeholder "GitHub username"}]]
          [:i.search.link.icon]]]]]
    [:main
     content]]))

(defn home []
  (with-layout
    [:div.ui.segment
     [:div.ui.center.aligned.stackable.very.relaxed.page.grid
      [:div.row
       [:div.fourteen.wide.column
        [:h1.ui.header "Hubfolio"]
        [:p "A portfolio of your most relevant GitHub repos"]
        [:div.ui.search
         [:form {:action "/" :method "post"}
          (anti-forgery-field)
          [:div.ui.icon.input
           [:input.prompt {:type "text"
                           :name "username"
                           :placeholder "GitHub username"}]
           [:i.search.icon]]]]]]]]))

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

(defn no-user [username]
  (with-layout
    [:div.ui.stackable.segment
     [:div.ui.stackable.center.aligned.page.grid
      [:div.fourteen.wide.column
       [:h1.ui.header (str username " not found")]
       [:p (str "There doesn't appear to be a GitHub account for " username ".")]]]]))

(defn failed [username]
  (with-layout
    [:div.ui.stackable.segment
     [:div.ui.stackable.center.aligned.page.grid
      [:div.fourteen.wide.column
       [:h1.ui.header "Oh no!"]
       [:p (str "Alas, generating statistics for " username " has failed")]
       [:a.ui.labeled.primary.button {:href "mailto:bruz.marzolf@gmail.com"} "Let someone know"]]]]))

(defn generating [username]
  (with-layout
    [:div
     [:div.ui.stackable.segment
      [:div.ui.stackable.center.aligned.page.grid
       [:div.fourteen.wide.column
        [:div.ui.active.inverted.dimmer
         [:div.ui.indeterminate.text.active.loader "Generating... this may take a while"]]
        [:br]
        [:br]
        [:br]
        [:br]
        [:br]]]]
     [:script "$(document).ready(function(){ checkStatus(); });"]]))

(defn repo-cards [username repos]
  [:div.ui.column
   [:div.ui.cards
    (for [repo repos]
      [:div.ui.card
       [:div.content
        [:div.right.floated
         [:div.ui.top.right.attached.green.label {:data-content "Combination of starshare, user commits and time since last commit"} "Score"
          [:div.detail (format-imprecise (repo :score))]]]
        [:a.header {:href (repo :html_url) :target "_blank"}
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
          [:div.ui.small.label {:data-content "Number of years since the last commit by anyone to this repo"} "Last commit age"
           [:div.detail
            [:i.wait.icon]
            (repo :stale-years)]]
          [:div.right.floated
           [:div.ui.small.label {:data-content "Total commits by all users to this repo"} "Total commits"
            [:div.detail
             [:i.users.icon]
             (repo :total-commits)]]]]]]])]])

(defn no-repos []
  [:div.ui.center.aligned.column
   [:h3 "No repos with commits found"]])

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
          (if (empty? repos)
            (no-repos)
            (repo-cards username repos))]]]])))

(defn user [username stats-conn generator github-auth store-config]
  (let [status (user-status/get-status generator github-auth store-config username)]
    (case
      status
      :not-opted-in (not-opted-in username)
      :no-user (no-user username)
      :failed (failed username)
      :generating (generating username)
      :generated (generated username store-config))))

(defn faq []
  (with-layout
    [:div.ui.stackable.segment
     [:div.ui.stackable.center.aligned.page.grid
      [:div.center.aligned.fourteen.wide.column
       [:h1.ui.header "Frequently Asked Questions"]]
      [:div.left.aligned.fourteen.wide.column
       [:div.ui.list
        [:div.item
         [:div.header
          "Why do I have to star the "
          [:a {:href "https://github.com/bruz/hubfolio"} "Hubfolio repo"]
          " to have my stats generated?"]
         "It's opt-in since not everyone wants their GitHub account to be seen
          as a portfolio of their work. Starring the repo is a convenient way
          to prove ownership of your GitHub account."]
        [:div.item
         [:div.header "How are the different metrics calculated?"]
         [:ul
          [:li "User commits: Number of commits contributed to the master
           branch of the repo by the user in approximately the last year."]
          [:li "Total commits: Total number of commits by all users to the
           master branch of the repo in approximately the last year."]
          [:li "Last commit age: Number of years since anyone has made
          a commit to the repo"]
          [:li "Starshare: \\[\\frac{commits_{user}}{commits_{total}}\\times{stars}\\]"]
          [:li "Score: \\[log_2(starshare)\\times log_2(commits_{user})\\times \\frac{1}{2^{age}}\\]"]]
         "For more details, see the "
         [:a {:href "https://github.com/bruz/hubfolio"} "source on GitHub"]]]]]]))


(defn create-routes [stats-conn generator github-auth store-config]
  (routes
    (GET "/" [] (home))
    (route/files "/" {:root "public"})
    (GET "/faq" [] (faq))
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
