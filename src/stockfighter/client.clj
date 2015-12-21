(ns stockfighter.client
    (:require [clj-http.client :as client]
              [environ.core :refer [env]]
              [cheshire.core :refer [parse-string generate-string]]
              [aleph.http :as http]
              [manifold.stream :as s]))

;; (require '(stockfighter [client :as c]))
;; or
;; (use 'stockfighter.client :reload)

(def protocol "https")
(def ws-protocol "wss")
(def host "api.stockfighter.io")
(def endpoints {
  :api-heartbeat "/ob/api/heartbeat"
  :venue-heartbeat (fn [venue] (str "/ob/api/venues/" venue "/heartbeat"))
  :stocks (fn [venue] (str "/ob/api/venues/" venue "/stocks"))
  :orderbook (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock))
  :stock-quote (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock "/quote"))
  :order (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock "/orders"))
  :order-status (fn [venue stock order-id] (str "/ob/api/venues/" venue "/stocks/" stock "/orders/" order-id))
  :ticker-tape (fn [venue stock account] (str "/ob/api/ws/" account "/venues/" venue "/tickertape/stocks/" stock))
  :cancel-order (fn [venue stock order-id] (str "/ob/api/venues/" venue "/stocks/" stock "/orders/" order))
})

(def api-key
  "Get the API key from environment variable STARFIGHTER_API_KEY"
  (env :starfighter-api-key))

(def headers {"X-Starfighter-Authorization" api-key})

(defn get-url [path]
  (str protocol "://" host path))

(defn get-ws-url [path]
  (str ws-protocol "://" host path))

(defn send-request
  ([endpoint]
    (client/get (get-url (endpoint endpoints))
      {:headers headers}
      {:as :clojure
       :throw-entire-message? true
       :debug true
       :debug-body true}))
  ([endpoint venue]
    (client/get (get-url ((endpoint endpoints) venue))
      {:headers headers}
      {:as :clojure
       :throw-entire-message? true
       :debug true
       :debug-body true}))
  ([endpoint venue stock]
     (client/get (get-url ((endpoint endpoints) venue stock))))
  ([endpoint venue stock order-id]
     (client/get (get-url ((endpoint endpoints) venue stock order-id))
                 {:headers headers
                  :throw-entire-message? true
                  :debug true
                  :debug-body true})))

(defn send-post [endpoint venue stock body]
  (client/post (get-url ((endpoint endpoints) venue stock))
               {:headers headers
                :body (generate-string body)
                :body-encoding "UTF-8"
                :throw-entire-message? true
                :debug true
                :debug-body true
                }))

(defn body [response]
  (parse-string (:body response)))

(defn api-heartbeat []
  (send-request :api-heartbeat))

(defn venue-heartbeat [venue]
  (send-request :venue-heartbeat venue))

(defn stocks [venue]
  (send-request :stocks venue))

(defn orderbook [venue stock]
  (send-request :orderbook venue stock))

(defn stock-quote [venue stock]
  (send-request :stock-quote venue stock))

(defn order [venue stock body]
  (send-post :order venue stock body))

(defn order-body [account venue stock qty price direction orderType]
  {:account account
   :venue venue
   :stock stock
   :qty qty
   :price price
   :direction direction
   :orderType orderType})

(defn order-status [venue stock order-id]
  (send-request :order-status venue stock order-id))

(defn cancel-order [venue stock order-id]
  (let [path ((:cancel-order endpoints) venue stock order-id)
        url (get-url path)
        resp (client/delete url)]
    (body resp)))


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

(defn ticker-tape-url [venue stock account]
  (get-ws-url ((endpoints :ticker-tape) venue stock account)))

(defn ticker-tape [venue stock account]
  (let [url (ticker-tape-url venue stock account)
        conn @(http/websocket-client url)]
    (prn @(s/take! conn))))

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

