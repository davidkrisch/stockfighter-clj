(ns stockfighter.client
    (:require [clj-http.client :as client]
              [environ.core :refer [env]]
              [cheshire.core :refer [parse-string]]))

;; (require '(stockfighter [client :as c]))
;; or
;; (use 'stockfighter.client :reload)

(def protocol "https")
(def host "api.stockfighter.io")
(def endpoints {
  :api-heartbeat "/ob/api/heartbeat"
  :venue-heartbeat (fn [venue] (str "/ob/api/venues/" venue "/heartbeat"))
  :stocks (fn [venue] (str "/ob/api/venues/" venue "/stocks"))
})

(def api-key
  "Get the API key from environment variable STARFIGHTER_API_KEY"
  (env :starfighter-api-key))

(def headers {:headers {"X-Starfighter-Authorization" api-key}})

(defn get-url [endpoint]
  (str protocol "://" host endpoint))

(defn send-request
  ([endpoint]
    (client/get (get-url (endpoint endpoints))
      headers
      {:as :clojure}))
  ([endpoint venue]
    (client/get (get-url ((endpoint endpoints) venue))
      headers
      {:as :clojure})))

(defn body [response]
  (parse-string (:body response)))

(defn api-heartbeat []
  (send-request :api-heartbeat))

(defn venue-heartbeat [venue]
  (send-request :venue-heartbeat venue))

(defn stocks [venue]
  (send-request :stocks venue))

(defn stocks-vec
  "Pull the `symbols` vector out of the body of the `stocks` response"
  [stocks-resp]
  ((body stocks-resp) "symbols"))
