(ns stockfighter.level4
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [manifold.stream :as stream]
            [stockfighter.client :as client]
            [stockfighter.state :as state]))

;; --------------------------
;; Level 4 Dueling Bulldozers
;; --------------------------

; Working code, but it doesn't win the level yet

(defn- trade [sys qty price dir]
  (let [{:keys [account venue stock]} @sys
        b (client/order-body account venue stock qty price dir "limit")]
    {:internal-id (state/uuid)
     :request-body b
     :response (client/order venue stock b)}))

(defn update-order-status
  "In a future, call order status until order is closed
  Update the trade with the fully filled order status"
  [sys trade]
  (future
    (let [internal-id (:internal-id trade)
          resp @(:response trade)
          body (client/body resp)
          {:keys [venue stock]} @sys]
      (when (= (:status resp) 200)
        (loop []
          (let [status @(client/order-status venue stock (:id body))
                order-status-body (client/body status)]
            (swap! sys state/update-trade internal-id order-status-body)
            (when-not (:open order-status-body)
              (log/info order-status-body))
            (when (:open order-status-body)
              (Thread/sleep 1200)
              (recur))))))))

(defn- mmmm [sys dir price]
  (let [s (state/should-trade2? sys dir)]
    (when (:ok s)
      (let [the-trade (trade sys (:qty s) price dir)]
        (swap! sys state/add-trade the-trade)
        (update-order-status sys the-trade)))))

(defn- make-orders [sys msg]
  (when (:ok msg)
    (let [{:keys [venue stock trades]} @sys
          {{:keys [bid ask]} :quote} msg]
      (when (and (not-any? nil? [bid ask])
                 (< 10 (- ask bid)))
        (mmmm sys "buy" (+ bid 5))
        (mmmm sys "sell" (- ask 5))
        (Thread/sleep 1000)
        (log/info (state/position sys))))))

(defn stream-quotes [system]
  (let [ticker-chan (:ticker-chan @system)]
    (stream/consume (partial make-orders system) ticker-chan)))
