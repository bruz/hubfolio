(ns hubfolio.generator
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async :refer [go go-loop chan <! >!]]
            [hubfolio.github :as github]
            [hubfolio.statistics :as stats]
            [hubfolio.user-data :as user-data]
            [hubfolio.user-status :as user-status]
            [schejulure.core :refer [schedule cron-of]]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.local :as l]))

(defn cronmap [at]
  (let [cron (cron-of at)]
    (select-keys (zipmap [:minute :hour :date :month :day] cron) [:hour :minute])))

(defn generate-at [generator-chan username at]
  (let [cron-time (cronmap at)]
    (schedule cron-time #(go (>! generator-chan username)))))

(defn reschedule [[username last-updated] generator-chan]
  (try
    (let [last-updated-time (l/to-local-date-time last-updated)
          next-at (t/plus last-updated-time (t/hours 24))]
      (println (str "Rescheduling " username " for " next-at))
      (generate-at generator-chan username next-at))
    (catch IllegalArgumentException e (println (str "Skipping " username)))))

(defn reschedule-all
  "Reschedule generation of statistics for all current users. Useful when
   previously-scheduled jobs are lost (e.g. after restart)."
  [generator-chan store-config]
  (let [users (user-status/all-last-updated store-config)]
    (doseq [user users] (reschedule user generator-chan))))

(defn timestamp []
  (f/unparse (f/formatters :date-time) (t/now)))

(defn generate [stats-conn store-config username]
  (println (str "Generating stats: " username))
  (let [data {:user (stats/user stats-conn username)
              :repos (stats/repos stats-conn username)}]
    (user-data/save store-config username data)))

(defn log-rate-limit [conn]
  (let [data (github/rate-limit conn)
        limit (get-in data [:resources :core :limit])
        remaining (get-in data [:resources :core :remaining])]
    (println (str "Rate limit: " remaining " of " limit " remaining"))))

(defn generator [conn store-config]
  (let [generator-chan (chan)]
    (go-loop []
             (let [username (<! generator-chan)
                   updated-at (timestamp)]
               (generate conn store-config username)
               (user-status/set-last-updated store-config username updated-at)
               (reschedule [username updated-at] generator-chan)
               (log-rate-limit conn))
             (recur))
    generator-chan))

(defrecord Generator [stats-conn store-config]
  component/Lifecycle

  (start [component]
         (let [generator-chan (generator stats-conn store-config)]
           (reschedule-all generator-chan store-config)
           (assoc component
             :chan generator-chan)))
  (stop [component]
        (assoc component
          :chan nil)))

(defn new-generator [store-config]
  (map->Generator {:store-config store-config}))
