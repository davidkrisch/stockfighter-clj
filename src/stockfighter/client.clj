(ns stockfighter.client
    (:require [clj-http.client :as client]
              [environ.core :refer [env]]
              [cheshire.core :refer [parse-string generate-string]]
              [slingshot.slingshot :refer [throw+ try+]]))

;; (require '(stockfighter [client :as c]))
;; or
;; (use 'stockfighter.client :reload)

(def protocol "https")
(def host "api.stockfighter.io")
(def endpoints {
  :api-heartbeat "/ob/api/heartbeat"
  :venue-heartbeat (fn [venue] (str "/ob/api/venues/" venue "/heartbeat"))
  :stocks (fn [venue] (str "/ob/api/venues/" venue "/stocks"))
  :orderbook (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock))
  :stock-quote (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock "/quote"))
  :order (fn [venue stock] (str "/ob/api/venues/" venue "/stocks/" stock "/orders"))
  :order-status (fn [venue stock order-id] (str "/ob/api/venues/" venue "/stocks/" stock "/orders/" order-id))
})

(def api-key
  "Get the API key from environment variable STARFIGHTER_API_KEY"
  (env :starfighter-api-key))

(def headers {"X-Starfighter-Authorization" api-key})

(defn get-url [endpoint]
  (str protocol "://" host endpoint))

(defn send-request
  ([endpoint]
    (client/get (get-url (endpoint endpoints))
      {:headers headers}
      {:as :clojure}))
  ([endpoint venue]
    (client/get (get-url ((endpoint endpoints) venue))
      {:headers headers}
      {:as :clojure}))
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


(defn level2 [account venue stock price]
  (dotimes [n 100]
    (let [body (order-body account venue stock price 1000 "buy" "fill-or-kill")]
      (prn n " ---> " body)
      (order venue stock body))))


(defn stocks-vec
  "Pull the `symbols` vector out of the body of the `stocks` response"
  [stocks-resp]
  ((body stocks-resp) "symbols"))
