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
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [cheshire.core :refer [parse-string generate-string]]
            [stockfighter.client :as client :refer :all]
            [stockfighter.core :as system]
            [stockfighter.level4 :refer [do-it]]))

;; To run in the repl: (reset)

(defn- trade [sys qty price dir]
  (let [{:keys [account venue stock]} @sys
        b (order-body account venue stock qty price dir "limit")]
    (order venue stock b)))

(defn quote-and-order [sys qty dir]
  (let [{:keys [venue stock]} @sys
        price-kw (if (= "buy" dir) :bid :ask)]
    @(d/chain' (stock-quote venue stock)
               client/body
               price-kw
               #(trade sys qty % dir)
               client/body
               :id)))

(defn buy-and-sell [sys qty]
  [(quote-and-order sys qty "sell")
   (quote-and-order sys qty "buy")])

(defn sss [sys order-id]
  (let [{:keys [venue stock]} @sys]
    @(d/chain' (client/order-status venue stock order-id)
               client/body)))

;(map (partial sss sys) (buy-and-sell sys 100))

(def system nil)

(defn init
  "Initialize system, but don't start it running"
  []
  (let [venue "EDLBEX"
        stock "WPYI"
        account "BHK96929815"]
    (alter-var-root #'system
                    (constantly (system/make-system venue stock account do-it)))))

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
