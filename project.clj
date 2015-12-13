(defproject stockfighter "0.1.0-SNAPSHOT"
  :description "My attempt at Stockfighter in Clojure"
  :url "http://example.com/FIXME"
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
                 ]
  :main ^:skip-aot stockfighter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
