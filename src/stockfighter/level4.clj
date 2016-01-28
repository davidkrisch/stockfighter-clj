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
        (swap! sys state/add-trade trade)
        (loop []
          (let [status @(client/order-status venue stock (:id body))
                order-status-body (client/body status)]
            (log/info ">>> Order status: " order-status-body)
            (swap! sys state/update-trade internal-id order-status-body)
            (log/info ">>> After swap! <<<")
            (when (:open order-status-body)
              (Thread/sleep 2000)
              (recur))))))))

(defn- make-orders [sys qty msg]
  (let [{:keys [venue stock trades]} @sys
        {:keys [ok] {:keys [bid ask]} :quote} msg]
    ; TODO do something with ok
    (when (and (not-any? nil? [bid ask])
               (every? #(> % 0) [bid ask]))
      (when (state/should-trade? sys "sell")
        (let [sell-trade (trade sys qty ask "sell")]
          (log/info "Sell Sell Sell" sell-trade)
          (update-order-status sys sell-trade)))
      (when (state/should-trade? sys "buy")
        (let [buy-trade (trade sys qty bid "buy")]
          (log/info "Buy Buy Buy" buy-trade)
          (update-order-status sys buy-trade))))))

(defn stream-quotes [system]
  (let [ticker-chan (:ticker-chan @system)]
    (stream/consume (partial make-orders system 250) ticker-chan)))
