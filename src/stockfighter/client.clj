(ns stockfighter.client
  (:require [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [aleph.http :as http]
            [manifold.stream :as s]
            [byte-streams :as bs]
            [cheshire.core :refer [parse-string generate-string]]))

(def protocol "https")
(def ws-protocol "wss")
(def host "api.stockfighter.io")
(def endpoints {:api-heartbeat "/ob/api/heartbeat"
                :venue-heartbeat (fn [venue] (str "/ob/api/venues/" venue "/heartbeat"))
                :stocks (fn [venue] (str "/ob/api/venues/" venue "/stocks"))
                :orderbook (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock))
                :stock-quote (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock "/quote"))
                :order (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock "/orders"))
                :order-status (fn [venue stock order-id] (str "/ob/api/venues/" venue "/stocks/" stock "/orders/" order-id))
                :ticker-tape (fn [venue stock account] (str "/ob/api/ws/" account "/venues/" venue "/tickertape/stocks/" stock))
                :fills (fn [venue stock account] (str "/ob/api/ws/" account "/venues/" venue "/executions/stocks/" stock))
                :cancel-order (fn [venue stock order-id] (str "/ob/api/venues/" venue "/stocks/" stock "/orders/" order-id))})

(def api-key (env :starfighter-api-key))

(def headers {"X-Starfighter-Authorization" api-key})

(defn get-url [path]
  (str protocol "://" host path))

(defn get-ws-url [path]
  (str ws-protocol "://" host path))

(defn ticker-tape-url [venue stock account]
  (get-ws-url ((endpoints :ticker-tape) venue stock account)))

(defn fills-url [venue stock account]
  (get-ws-url ((endpoints :fills) venue stock account)))

; Debug options
;{:as :clojure
;       :throw-entire-message? true
;       :debug true
;       :debug-body true}

(defn send-request
  ([endpoint]
   (http/get (get-url (endpoint endpoints))
             {:headers headers}))
  ([endpoint venue]
   (http/get (get-url ((endpoint endpoints) venue))
             {:headers headers}))
  ([endpoint venue stock]
     (http/get (get-url ((endpoint endpoints) venue stock))
               {:headers headers}))
  ([endpoint venue stock order-id]
   (http/get (get-url ((endpoint endpoints) venue stock order-id))
             {:headers headers})))

(defn send-post [endpoint venue stock body]
  (http/post (get-url ((endpoint endpoints) venue stock))
               {:headers headers
                :body (generate-string body)
                :body-encoding "UTF-8"}))

(defn body [response]
  (->
    response
    :body
    (bs/convert String)
    parse-string
    clojure.walk/keywordize-keys))

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
  (let [path ((:cancel-order endpoints) venue stock order-id)]
    (http/delete (get-url path) {:headers headers})))

; Here be Websockets

(defn parse-message [msg]
  (->
    msg
    parse-string
    clojure.walk/keywordize-keys))

(defn- consume-websocket
  "Wait for messages on `ws-conn`, put them on `channel` when they arrive"
  [ws-conn channel]
   (let [the-stream (s/map parse-message ws-conn)]
     (s/connect the-stream channel)))

(defn ticker
  "
  Connect to stock-ticker websocket and jam results
  onto (:ticker-chan system) until the connection closes
  or ticker-chan closes
  "
  [system]
  (let [{:keys [ticker-ws-conn ticker-chan]} @system]
    (consume-websocket ticker-ws-conn ticker-chan)))

(defn executions
  "
  Connect to the Executions websocket and jam results
  onto (:fills-chan system) until the connection closes
  or fills-chan closes.
  "
  [system]
  (let [{:keys [fills-ws-conn fills-chan]} @system]
    (consume-websocket fills-ws-conn fills-chan)))
