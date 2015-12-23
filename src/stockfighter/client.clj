(ns stockfighter.client
    (:require [clj-http.client :as client]
              [environ.core :refer [env]]
              [cheshire.core :refer [parse-string generate-string]]))

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
  :cancel-order (fn [venue stock order-id] (str "/ob/api/venues/" venue "/stocks/" stock "/orders/" order-id))
})

(def api-key
  "Get the API key from environment variable STARFIGHTER_API_KEY"
  (env :starfighter-api-key))

(def headers {"X-Starfighter-Authorization" api-key})

(defn get-url [path]
  (str protocol "://" host path))

(defn get-ws-url [path]
  (str ws-protocol "://" host path))

(defn ticker-tape-url [venue stock account]
  (get-ws-url ((endpoints :ticker-tape) venue stock account)))

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
   (let [url (get-url ((endpoint endpoints) venue stock))]
     (client/get url {:headers headers})))
  ([endpoint venue stock order-id]
     (client/get (get-url ((endpoint endpoints) venue stock order-id))
                 {:headers headers})))

(defn send-post [endpoint venue stock body]
  (client/post (get-url ((endpoint endpoints) venue stock))
               {:headers headers
                :body (generate-string body)
                :body-encoding "UTF-8"
                :throw-entire-message? true
                :debug true
                :debug-body true}))

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
        resp (client/delete url {:headers headers
                                 :throw-entire-message? true
                                 :debug true
                                 :debug-body true})]
    (body resp)))





