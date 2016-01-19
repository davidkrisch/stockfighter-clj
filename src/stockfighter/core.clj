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
   :fills-chan nil
   :ticker-ws-conn nil
   :fills-ws-conn nil
   :solution-fn solution-fn})

(defn- sliding-buf-chan [size]
  (async/chan (async/sliding-buffer size)))

(defn- ws-client [url]
  @(http/websocket-client url))

(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [system]
  (println "Starting the system")
  (let [{:keys [venue stock account]} system
        ticker-url (client/ticker-tape-url venue stock account)
        fills-url (client/fills-url venue stock account)
        ticker-ws-conn (ws-client ticker-url)
        fills-ws-conn (ws-client fills-url)
        ticker-chan (sliding-buf-chan 1)
        fills-chan (sliding-buf-chan 100) ; TODO is sliding buffer okay?
        s (assoc system :ticker-chan ticker-chan
                 :fills-chan fills-chan
                 :ticker-ws-conn ticker-ws-conn
                 :fills-ws-conn fills-ws-conn)]
    (client/executions s)
    (client/ticker s)
    ((:solution-fn s) s)
    s))

(defn- close-ws-conn! [conn]
  (if conn (stream/close! conn)))

(defn- close-chan! [chan]
  (if chan (async/close! chan)))

(defn stop
  "Performs side effects to shut down the system and release its
  resources. Returns an updated instance of the system."
  [system]

  (close-chan! (:ticker-chan system))
  (close-chan! (:fills-chan system))
  (close-ws-conn! (:ticker-ws-conn system))
  (close-ws-conn! (:fills-ws-conn system))

  (println "Stopping for now")
  (assoc system :ticker-chan nil
         :fills-chan nil
         :ticker-ws-conn nil
         :fills-ws-conn nil))
