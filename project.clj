(defproject org.clojars.jcrossley3/poorpus "0.1.1-SNAPSHOT"
  :description "Encapsulates clj-http/twitter-api for Poorsmatic"
  :url "https://github.com/jcrossley3/poorpus"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.immutant/immutant "0.5.0" :scope "provided"]
                 [clj-http "0.5.5"]
                 [twitter-api "0.6.12"]
                 [clj-time "0.3.7"]]
  :immutant {:swank-port 4005}
  :lein-release {:deploy-via :clojars})
