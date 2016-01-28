(ns stockfighter.state
  (require [manifold.stream :as stream]
           [clojure.tools.logging :as log]
           [stockfighter.client :as client]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn add-trade
  "Append trade to (:trades sys)"
  [sys trade]
  (update-in sys [:trades] conj trade))

(defn- id [trade]
  (-> :response
      trade
      deref
      client/body
      :id))

(defn by-id
  "Get trade in (:trades @sys) where id of trade is order-id"
  [sys order-id]
  {:pre [(instance? clojure.lang.Atom sys)]}
  (first (filter #(= order-id
                     (id %))
                 (:trades @sys))))

(defn index-of [trades id]
  (let [pred #(= (:internal-id %) id)]
    (first (keep-indexed (fn [i x] (when (pred x) i))
                         trades))))

(defn update-trade
  "Update trade with order-status"
  [sys internal-id update]
  (log/info "in update-trade >>>" internal-id "<<<" update)
  (let [idx (index-of sys internal-id)]
    (log/info "&&&&& update-trade-idx" idx)
    (update-in sys [:trades idx] assoc :status update)))

(defn should-trade?
  "Make a decision if a trade is a good idea right now"
  [sys dir]
  {:pre [(instance? clojure.lang.Atom sys)]}
    (< (count (:trades @sys)) 4))

(defn handle-fill
  "Update system with execution ticker message.
  This should be used as the update fn in reset!"
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
    (log/info "FILL: order-id:" id
             "; dir:" direction
             "; filled:" filled
             "; profit:" (profit sys))))

(defn- handle-message [sys msg]
  (log-fill sys msg)
  (reset! sys (handle-fill sys msg))
  (log/info "**** Inventory: " (:inventory @sys)))

(defn fill-resp
  "Pull fill message from websocket and update the system"
  [sys]
  (let [fills-chan (:fills-chan @sys)
        handler (partial handle-message sys)]
    (stream/consume handler fills-chan)))
