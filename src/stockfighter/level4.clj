(ns stockfighter.level4
  (:require [clojure.core.async :as async]
            [stockfighter.client :as client]))

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
        qty 10
        order-type "limit"
        b (client/order-body account venue stock qty price direction order-type)
        resp (client/body (client/order venue stock b))]
    (println (format ">>>>>>>> Request: %s" b))
    (println (format ">>> Response: %s" resp))))

(defn do-it [system]
  (let [ticker-chan (:ticker-chan system)]
    (async/thread
      (loop []
        (when-let [m (async/<!! ticker-chan)]
          (when-let [ask (-> :quote m :ask)]
            (println (format "Ask: %5d\n" ask))
            (do-orders system (- ask 5) "sell"))
          (when-let [bid (-> :quote m :bid)]
            (println (format "Bid: %5d\n" bid))
            (do-orders system (+ bid 5) "buy"))
          (recur))))))
