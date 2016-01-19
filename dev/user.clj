(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.core.async :as async]
            [aleph.http :as http]
            [manifold.stream :as s]
            [cheshire.core :refer [parse-string generate-string]]
            [stockfighter.client :as client :refer :all]
            [stockfighter.core :as system]
            [stockfighter.level4 :refer [do-it]]))

;; To run in the repl: (reset)

(def system nil)

(defn init
  "Initialize system, but don't start it running"
  []
  (let [venue "QEITEX"
        stock "SEI"
        account "SSB92351643"
        test-fn do-it]
    (alter-var-root #'system
                    (constantly (system/make-system venue stock account test-fn)))))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (system/stop s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (alter-var-root #'system system/start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
