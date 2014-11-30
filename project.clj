(defproject hubfolio "0.1.0-SNAPSHOT"
  :description "Generate portfolios from GitHub"
  :url "http://hubfol.io"
  :license {:name "TODO: Choose a license"
            :url "http://choosealicense.com/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [tentacles "0.2.5"]
                 [compojure "1.2.1"]
                 [hiccup "1.0.5"]
                 [ring "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [com.taoensso/carmine "2.7.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.7"]]
                   :source-paths ["dev"]}})
  ;;:plugins [[lein-ring "0.8.13"]]
  ;;:ring {:handler hubfolio.server/new-server})
