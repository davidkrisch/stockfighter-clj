(ns stockfighter.state
  (require [clojure.core.async :as async]
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

(defn- log-fill [m]
  (let [{:keys [order filled] {:keys [id direction]} :order} m]
    (println "FILL: order-id:" id
             "; dir:" direction
             "; filled:" filled)))

(defn fill-resp
  "Pull fill message from websocket a update system"
  [system]
  (println "^^^Starting fills responder thread!!!!")
  (let [fills-chan (:fills-chan @system)]
    (async/thread
      (loop []
        (when-let [m (async/<!! fills-chan)]
          (log-fill m)
          (reset! system (handle-fill system m))
          (println "**** Inventory: " (:inventory @system))
          (recur)))
      (println "^^^ Stopping fills responder thread!!!"))))
