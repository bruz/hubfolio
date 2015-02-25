(defproject hubfolio "0.1.0-SNAPSHOT"
  :description "Generate portfolios from GitHub"
  :url "http://hubfol.io"
  :license {:name "TODO: Choose a license"
            :url "http://choosealicense.com/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [org.clojars.bruz/tentacles "0.3.0.1"]
                 [compojure "1.2.1"]
                 [hiccup "1.0.5"]
                 [ring "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [com.taoensso/carmine "2.7.0" :exclusions [org.clojure/clojure]]
                 [hiccup "1.0.5"]
                 [clj-time "0.6.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [schejulure "1.0.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.7"]]
                   :source-paths ["dev"]}}
  :main hubfolio.core)
