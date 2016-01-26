(ns stockfighter.state
  (require [manifold.stream :as stream]))

(defn add-trade
  "Append trade to (:trades sys)"
  [sys trade]
  (update-in sys [:trades] conj trade))

(defn- id [trade]
  (-> :response
      trade
      :id))

(defn by-id
  "Get trade in (:trades @sys) where id of trade is order-id"
  [sys order-id]
  {:pre [(instance? clojure.lang.Atom sys)]}
  (first (filter #(= order-id
                     (id %))
                 (:trades @sys))))

(defn index-of
  "Find the index in :trades of the given order-id"
  [sys order-id]
  (let [predicate #(= order-id (id %))
        trades (:trades sys)]
    (first (keep-indexed (fn [i x] (when (predicate x) i))
                         trades))))

(defn update-trade
  "Update trade record with status message"
  [sys update]
  (let [idx (index-of sys (:id update))]
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
