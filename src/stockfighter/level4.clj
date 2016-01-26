(ns stockfighter.level4
  (:require [clojure.core.async :as async]
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
    {:request-body b
     :response (client/order venue stock b)}))

(defn update-order-status
  "In a future, call order status until order is closed
  Update the trade with the fully filled order status"
  [sys trade]
  (future
    (let [resp @(:response trade)
          body (client/body resp)
          {:keys [venue stock]} @sys]
      (when (= (:status resp) 200)
        (loop []
          (let [status @(client/order-status venue stock (:id body))
                order-status-body (client/body status)]
            (println ">>> Order status: " order-status-body)
            (swap! sys state/update-trade order-status-body)
            (when (:open order-status-body)
              (recur))))))))

(defn- make-orders [sys qty msg]
  (let [{:keys [venue stock trades]} @sys
        {:keys [ok] {:keys [bid ask]} :quote} msg]
    (when (and (not-any? nil? [bid ask])
               (every? #(> % 0) [bid ask]))
      (when (state/should-trade? sys "sell")
        (let [sell-trade (trade sys qty ask "sell")]
          (println "Sell Sell Sell" sell-trade)
          (swap! sys state/add-trade sell-trade)
          (update-order-status sys sell-trade)))
      (when (state/should-trade? sys "buy")
        (let [buy-trade (trade sys qty bid "buy")]
          (println "Buy Buy Buy" buy-trade)
          (swap! sys state/add-trade buy-trade)
          (update-order-status sys buy-trade))))))

(defn stream-quotes [system]
  (let [ticker-chan (:ticker-chan @system)]
    (stream/consume (partial make-orders system 250) ticker-chan)))
