(ns stockfighter.client
    (:require [clj-http.client :as client]
              [environ.core :refer [env]]))

;; (require '(stockfighter [client :as c]))
;; or
;; (use 'stockfighter.client :reload)

(def protocol "https")
(def host "api.stockfighter.io")
(def endpoints {
  :api-heartbeat "/ob/api/heartbeat"
  :venue-heartbeat (fn [venue] (str "/ob/api/venues/" venue "/heartbeat"))
})

(def api-key
  "Get the API key from environment variable STARFIGHTER_API_KEY"
  (env :starfighter-api-key))

(defn get-url [endpoint]
  (str protocol "://" host endpoint))

(defn send-request [url]
  (client/get url
    {:headers {"X-Starfighter-Authorization" api-key}}))

(defn api-heartbeat []
  (send-request (get-url (:api-heartbeat endpoints))))

(defn venue-heartbeat [venue]
  (send-request (get-url ((:venue-heartbeat endpoints) venue))))
