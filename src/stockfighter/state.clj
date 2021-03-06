(ns stockfighter.state
  (require [manifold.stream :as stream]
           [clojure.tools.logging :as log]
           [stockfighter.client :as client]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn add-trade
  "Append trade to (:trades sys)"
  [sys trade]
  (update-in sys [:trades] conj trade))

;(defn- id [trade]
  ;(-> :response
      ;trade
      ;deref
      ;client/body
      ;:id))

;(defn by-id
  ;"Get trade in (:trades @sys) where id of trade is order-id"
  ;[sys order-id]
  ;{:pre [(instance? clojure.lang.Atom sys)]}
  ;(first (filter #(= order-id
                     ;(id %))
                 ;(:trades @sys))))

(defn index-of [trades id]
  (let [pred #(= (:internal-id %) id)]
    (first (keep-indexed (fn [i x] (when (pred x) i))
                         trades))))

(defn update-trade
  "Update trade with order-status"
  [sys internal-id update]
  (let [idx (index-of (:trades sys) internal-id)]
    (assoc-in sys [:trades idx :status] update)))

(defn- update-position
  "Update position with trade-status"
  [current-position trade-status]
  (if (some? trade-status)
    (let [{:keys [totalFilled price direction]} trade-status
          shares-fn (if (= direction "buy") + -)
          cash-fn (if (= direction "buy") - +)]
      (-> current-position
          (update :shares shares-fn totalFilled)
          (update :cash cash-fn (* price totalFilled))))
    current-position))

(defn position
  "Calculate cash & shares in hand.
  returns a map of the form {:shares 0 :cash 0}"
  [sys]
  {:pre [(instance? clojure.lang.Atom sys)]}
  (let [status-list (map :status (:trades @sys))
        init {:shares 0 :cash 0}]
    (reduce update-position init status-list)))

(defn- update-outstanding [outstanding-orders trade]
    (let [request-body (:request-body trade)
          {:keys [direction qty]} request-body
          shares-fn (if (= direction "buy") + -)]
      (shares-fn outstanding-orders qty)))

(defn outstanding [sys]
  "Position of outstanding orders (trades without status)"
  {:pre [(instance? clojure.lang.Atom sys)]}
  (let [trades (filter #(-> % :status nil?) (:trades @sys))]
    (reduce update-outstanding 0 trades)))

; {:ok true :num-shares 10}
(defn should-trade?
  "Is trading is a good idea right now?"
  [sys dir]
  {:pre [(instance? clojure.lang.Atom sys)
         (contains? #{"buy" "sell"} dir)]}
  (let [filled (:shares (position sys))
        out (outstanding sys)
        ok {:ok true :qty 10}
        not-ok {:ok false}]
    (if (= dir "buy")
      (if (< (+ filled out) 250) ok not-ok)
      (if (> (+ filled out) -250) ok not-ok))))

(defn open-orders
  "Returns a lazy sequence of open orders"
  [sys]
  {:pre [(instance? clojure.lang.Atom sys)]}
  (filter #(or (-> % :status nil?)
               (-> % :status :open))
          (:trades @sys)))

(defn is-out?
  "Is there an outstanding order in this direction?"
  [sys dir]
  (some #(= (-> %
                :request-body
                :direction)
            dir)
        (open-orders sys)))

(defn should-trade2? [sys dir]
  {:pre [(instance? clojure.lang.Atom sys)
         (contains? #{"buy" "sell"} dir)]}
  (log/info "Open orders: " (count (open-orders sys)))
  (if (is-out? sys dir)
    {:ok false}
    {:ok true :qty 200}))

; Functions to handle messages from fill websocket
;
; Working code, but not used, because it doesn't update
; the part of @sys used by position

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

(defn- log-fill [sys m]
  (let [{:keys [order filled] {:keys [id direction]} :order} m]
    (log/info "FILL: order-id:" id
             "; dir:" direction
             "; filled:" filled
             "; position:" (position sys))))

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
