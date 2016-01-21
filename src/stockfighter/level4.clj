(ns stockfighter.level4
  (:require [clojure.core.async :as async]
            [stockfighter.client :as client]
            [stockfighter.state :as state]))

;; --------------------------
;; Level 4 Dueling Bulldozers
;; --------------------------

; Simple Strategy
;  For each ticker quote
;    if bid in quote and sell?
;      sell @ ask - 5 x 100
;    if ask in quote and buy?
;      buy @ bid + 5 x 100

; sell? if :position > 0 (-100?)
; buy? if :position < 0 (+100?)

; State
; - Position - shares long/short & price
; - 1 outstanding buy order (order-id price num-shares)
; - 1 outstanding sell order ^^
; - $ made


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
    (reset! order resp) ; TODO put response in system:orders
    ; must do this so we can cancel orders that never fill
    (swap! inventory (partial inv-fn qty))))

(defn- price [q k]
  (let [p (-> :quote q k)]
    (if (= k :ask)
      (+ p 200)
      (- p 200))))

(defn do-it [system]
  (println "^^^Starting quote responder thread!!!!")
  (let [ticker-chan (:ticker-chan system)]
    (async/thread
      (loop []
        (when-let [m (async/<!! ticker-chan)]
          (if-not @(:order system)
            (do
              (if (pos? @(:inventory system))
                (do-orders system (price m :ask) "sell")
                (do-orders system (price m :bid) "buy"))))
          (recur))))))

; first attempt
;(defn do-it [system]
  ;(let [ticker-chan (:ticker-chan system)]
    ;(async/thread
      ;(loop []
        ;(when-let [m (async/<!! ticker-chan)]
          ;(when-let [ask (-> :quote m :ask)]
            ;(println (format "Ask: %5d\n" ask))
            ;(do-orders system (- ask 5) "sell"))
          ;(when-let [bid (-> :quote m :bid)]
            ;(println (format "Bid: %5d\n" bid))
            ;(do-orders system (+ bid 5) "buy"))
          ;(recur))))))
