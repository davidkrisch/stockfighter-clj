(ns stockfighter.core
  (:require [clojure.core.async :as async]
            [manifold.stream :as stream]
            [aleph.http :as http]
            [stockfighter.client :as client]))


(defn make-system
  "Returns a new instace of the whole application"
  [venue stock account solution-fn]
  {:venue venue
   :stock stock
   :account account
   :ticker-chan nil
   :ticker-ws-conn nil
   :solution-fn solution-fn})



(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [system]
  (println "Starting the system")
  (let [{:keys [venue stock account]} system
        url (client/ticker-tape-url venue stock account)
        ws-conn @(http/websocket-client url)
        ticker-chan (async/chan (async/sliding-buffer 1))
        s (assoc system :ticker-chan ticker-chan :ticker-ws-conn ws-conn)]
    (client/ticker s)
    ((:solution-fn s) s)
    s))


(defn stop
  "Performs side effects to shut down the system and release its
  resources. Returns an updated instance of the system."
  [system]
  (when-let [ticker-chan (:ticker-chan system)]
    (async/close! ticker-chan))
  (when-let [conn (:ticker-ws-conn system)]
    (stream/close! conn))
  (println "Stopping for now")
  (assoc system :ticker-chan nil :ticker-ws-conn nil))
