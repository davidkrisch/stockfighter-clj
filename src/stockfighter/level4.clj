(ns stockfighter.level4
  (:require [clojure.core.async :as async]
            [manifold.stream :as stream]
            [stockfighter.client :as client]
            [stockfighter.state :as state]))

;; None of this works yet.  Getting closer though

;; --------------------------
;; Level 4 Dueling Bulldozers
;; --------------------------

; sell? if :inventory > 0 (-100?)
; buy? if :inventory < 0 (+100?)

(defn- do-orders [system price direction]
  (let [{:keys [venue stock account]} system
        qty 100
        order-type "limit"
        b (client/order-body account venue stock qty price direction order-type)
        resp (client/body (client/order venue stock b))
        inv-fn (if (= "buy" direction) + -)
        order (:order system)
        inventory (:inventory system)]
    (println (format ">>>>>>>> %s: %s" direction b))
    (println (format ">>> Response: %s" resp))
    ; TODO put resp in system:orders to allow cancelling unfilled orders
    ; TODO update system:inventory with immediate fills
    ))

(defn- price [q k]
  (let [p (-> :quote q k)]
    (if (= k :ask)
      (+ p 200)
      (- p 200))))

;(if (pos? (:inventory @system))
  ;(do-orders system (price m :ask) "sell")
  ;(do-orders system (price m :bid) "buy"))))

(defn- handle-quote [msg]
  ; TODO do something besides log it
  (println "Ticker:" msg))

(defn do-it [system]
  (println "^^^Starting quote responder thread!!!!")
  (let [ticker-chan (:ticker-chan @system)]
    (stream/consume handle-quote ticker-chan)))
