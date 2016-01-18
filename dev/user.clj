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
            [stockfighter.client :as client :refer :all]))

;; (require '(stockfighter [client :as c]))
;; or
;; (use 'stockfighter.client :reload)

;; ----------------------------
;; Level 2 - Buy 100,000 shares
;; ----------------------------

(def totalFilled (atom 0))

; This one worked
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

; THIS WORKS
; stockfighter.core=> (def account "MAH91523492")
; #'stockfighter.core/account
; stockfighter.core=> (def stock "UPWY")
; #'stockfighter.core/stock
; stockfighter.core=> (def venue "IOKVEX")
; #'stockfighter.core/venue
; stockfighter.core=> (def url (ticker-tape-url venue stock account))
; #'stockfighter.core/url
; stockfighter.core=> url
; "wss://api.stockfighter.io/ob/api/ws/MAH91523492/venues/IOKVEX/tickertape/stocks/UPWY"
; stockfighter.core=> (def conn @(http/websocket-client url))
; #'stockfighter.core/conn
; stockfighter.core=> (prn @(s/take! conn))
; "{\"ok\":true,\"quote\":{\"symbol\":\"UPWY\",\"venue\":\"IOKVEX\",\"bid\":6034,\"ask\":6094,\"bidSize\":6018,\"askSize\":3782,\"bidDepth\":18054,\"askDepth\":11596,\"last\":6094,\"lastSize\":125,\"lastTrade\":\"2015-12-21T09:30:21.269035544Z\",\"quoteTime\":\"2015-12-21T09:30:21.26909563Z\"}}"
; nil
; END WORKING STUFF


;; (use 'stockfighter.client :reload)
;; (use 'user :reload)

; Simple Strategy
;  For each ticker quote
;    if bid in quote and sell?
;      sell @ ask - 5 x 100
;    if ask in quote and buy?
;      buy @ bid + 5 x 100

; State
; - Position - shares long/short & price
; - 1 outstanding buy order (order-id price num-shares)
; - 1 outstanding sell order ^^

(def system {
             :account "WAT42153333"
             :venue "DUWPEX"
             :stock "OLC"
             :chan-out (async/chan (async/sliding-buffer 1))
             :qty 1000
             :order-type "limit"
             :state (atom {:position 0})
             })

; Buy if :position < 0 (+100?)
; Sell if :position > 0 (-100?)

(defn do-orders [system price direction]
  (let [{:keys [venue stock account qty order-type]} system
        b (order-body account venue stock qty price direction order-type)]
    (println (format ">>>>> Request Body: %s" b))
    (println (format ">>>> Response Body: %s" (body (order venue stock b))))))

(defn lvl4b [system]
  (let [ticker-chan (:chan-out system)]
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
