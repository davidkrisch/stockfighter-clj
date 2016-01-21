(ns stockfighter.state
  (require [clojure.core.async :as async]
           [manifold.stream :as stream]
           [clojure.pprint :refer (pprint)]))

(defn handle-fill
  "Update system with execution ticker message.
  This should be used as the update fn in swap!"
  [sys fill]
  {:pre [(instance? clojure.lang.Atom sys)]}
  (when (:ok fill)
    (let [{:keys [order filled] {:keys [id direction]} :order} fill
          update-fn (if (= "buy" direction) + -)]
      (-> @sys
          (update-in [:orders] assoc id order)
          (update-in [:inventory] update-fn filled)))))

(defn profit
  "Calculate profit from the fills we've recorded"
  [sys]
  {:pre [(instance? clojure.lang.Atom sys)]}
  0)

(defn- log-fill [sys m]
  (let [{:keys [order filled] {:keys [id direction]} :order} m]
    (println "FILL: order-id:" id
             "; dir:" direction
             "; filled:" filled
             "; profit:" (profit sys))))

(defn- handle-message [sys msg]
  (log-fill sys msg)
  (reset! sys (handle-fill sys msg))
  (println "**** Inventory: " (:inventory @sys)))

(defn fill-resp
  "Pull fill message from websocket and update the system"
  [sys]
  (let [fills-chan (:fills-chan @sys)
        handler (partial handle-message sys)]
    (stream/consume handler fills-chan)))
