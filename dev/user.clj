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
            [stockfighter.client :as client :refer :all]))

;; (require '(stockfighter [client :as c]))
;; or
;; (use 'stockfighter.client :reload)

;; ----------------------------
;; Level 2 - Buy 100,000 shares
;; ----------------------------

(def totalFilled (atom 0))

(defn level2 [account venue stock price]
  (while (< @totalFilled 100000)
    (let [request-body (order-body account venue stock (rand-int 1000) price "buy" "immediate-or-cancel")
          response-body (body (order venue stock request-body))]
      (prn " ------> " request-body)
      (prn " ---------> " response-body)
      (if (contains? response-body "totalFilled")
        (let [filled (response-body "totalFilled")]
          (prn "********---> filled: " filled)
          (swap! totalFilled + filled))))))

;; ----------------------
;; Level 3 - Market Maker
;; ----------------------
;; Just sell a bunch of shares for a lot, they'll get bought

;; --------------------------
;; Level 4 Dueling Bulldozers
;; --------------------------
;(def uyum-buy (order-body account venue stock 10 2500 "buy" "limit"))
;(order venue stock uyum-buy)
;(def uyum-sell (order-body account venue stock 10 30000 "sell" "limit"))
;(order venue stock uyum-sell)

; None of the code below works

(defn get-ask [venue stock]
  (-> (stock-quote venue stock) body (get "ask")))

(defn get-bid [venue stock]
  (-> (stock-quote venue stock) body (get "bid")))

(def total (atom 0))
(def sold-price (atom 0))
(def bought-price (atom 0))

(defn level4 [account venue stock]
  (while (< @total 250000)
    (let [bid (get-bid venue stock)
          ask (get-ask venue stock)
          sell? (> @bought-price ask)
          buy? (< @sold-price bid)
          sell-body (order-body account venue stock 10 (- ask 5) "sell" "limit")
          buy-body (order-body account venue stock 10 (+ bid 5) "buy" "limit")]
      (if buy?
        (-> (order account venue stock buy-body) body))
      (if sell?
        (order account venue stock sell-body)))))



(def ticker-chan (async/chan))
(def bid (atom 0))
(def ask (atom 0))


(defn ticker-tape-B [venue stock account]
  (let [url (ticker-tape-url venue stock account)
        conn @(http/websocket-client url)]
    (prn @(s/take! conn))))


(defn ticker-tape-A [venue stock account]
  (let [url (ticker-tape-url venue stock account)
        conn @(http/websocket-client url)]
    (async/go
     (do
       (async/>! ticker-chan @(s/take! conn))
       (let [t (async/<! ticker-chan)]
         (prn t))))))


(defn ticker-tape [venue stock account]
  (let [url (ticker-tape-url venue stock account)
        conn @(http/websocket-client url)]
    (while true
      (async/go
        (do
          (async/>! ticker-chan @(s/take! conn))
          (let [t (async/<! ticker-chan)
                t-body (body t)
                {:strs [bid ask]} t-body]
            (prn "bid: " bid "; ask: " ask)))))))

; Market Maker Processes
;
; 1. Websocket listener - ticker-tape messages
;     - parse response
;     - put on chan
; 2. Get bid/ask from channel
;     - Compare bid to current buy order
;        - If bid is higher, put message on channel
;     - Compare ask to current sell order
;        - If ask is lower, put message on channel
; 3. Cancel order
; 4. Place 1 buy, 1 sell order
; 5. Websocket listener - fills
;     - If our order is complete, place another
;
; State
; - Current bid
; - Current ask
; - 1 outstanding buy order (order-id price num-shares)
; - 1 outstanding sell order ^^

