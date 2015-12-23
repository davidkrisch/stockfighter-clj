(defproject stockfighter "0.1.0-SNAPSHOT"
  :description "My attempt at Stockfighter in Clojure"
  :url "https://github.com/davidkrisch/stockfighter-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 ;; clj-http optional dependency for :as :clojure
                 [org.clojure/tools.reader "1.0.0-alpha1"]
                 ;; clj-http optional dependency for :as :json
                 [cheshire "5.5.0"]
                 [environ "1.0.1"]
                 [aleph "0.4.1-beta2"]
                 [org.clojure/core.async "0.2.374"]]
  :target-path "target/%s"
  :profiles {:dev {:source-paths ["dev"]
          :dependencies [[org.clojure/tools.namespace "0.2.3"]
                         [org.clojure/java.classpath "0.2.0"]]}})
