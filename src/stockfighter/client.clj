(ns stockfighter.client
  (:require [clj-http.client :as client]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [aleph.http :as http]
            [manifold.stream :as s]
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
   (client/get (get-url (endpoint endpoints))
               {:headers headers}))
  ([endpoint venue]
   (client/get (get-url ((endpoint endpoints) venue))
               {:headers headers}))
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
                :body-encoding "UTF-8"}))

(defn body [response]
  (clojure.walk/keywordize-keys (parse-string (:body response))))

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
        resp (client/delete url {:headers headers})]
    (body resp)))

; Here be Websockets

(defn- consume-websocket
  "Wait for messages on `ws-conn`, put them on `channel` when they arrive"
  ([ws-conn channel]
   (consume-websocket ws-conn channel false))
  ([ws-conn channel log-prefix]
   (async/thread
     (loop []
       (when-let [m (parse-string @(s/take! ws-conn))]
         (when-let [_ (async/>!! channel (clojure.walk/keywordize-keys m))]
           (when log-prefix (log/info log-prefix m))
           (recur)))))))

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
  (let [sys @system
        conn (:fills-ws-conn sys)
        fills-chan (:fills-chan sys)]
    (consume-websocket conn fills-chan)))
