(ns stockfighter.core
  (:require [clojure.core.async :as async]
            [manifold.stream :as stream]
            [aleph.http :as http]
            [stockfighter.client :as client]
            [stockfighter.state :as state]))

(defn make-system
  "Returns a new instace of the whole application"
  [venue stock account solution-fn old-sys]
  (let [trades (if (nil? old-sys) [] (:trades @old-sys))]
    (println "Existing trades " trades)
    (atom {:venue venue
           :stock stock
           :account account
           :ticker-chan nil
           :fills-chan nil
           :ticker-ws-conn nil
           :fills-ws-conn nil
           :solution-fn solution-fn
           :inventory 0 ; remove this, use :trades instead
           :orders {} ; remove this, use :trades instead
           :trades trades})))

(defn- sliding-buf-chan [size]
  (async/chan (async/sliding-buffer size)))

(defn- ws-client [url]
  @(http/websocket-client url))

(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [system]
  (println "Starting the system")
  (let [{:keys [venue stock account]} @system
        ticker-url (client/ticker-tape-url venue stock account)
        ticker-ws-conn (ws-client ticker-url)
        ticker-chan (sliding-buf-chan 1)
        ;fills-url (client/fills-url venue stock account)
        ;fills-ws-conn (ws-client fills-url)
        fills-chan (sliding-buf-chan 100)] ; TODO is sliding buffer okay?
    (swap! system assoc
           :ticker-chan ticker-chan
           :fills-chan fills-chan
           :ticker-ws-conn ticker-ws-conn)
           ;:fills-ws-conn fills-ws-conn)
    ;(stream/on-closed fills-ws-conn (println ">> fills websocket closed"))
    (stream/on-closed ticker-ws-conn (println ">> ticker websocket closed"))
    ;(client/executions system)
    ;(state/fill-resp system)
    (client/ticker system)
    ((:solution-fn @system) system)
    system))

(defn- close-ws-conn! [conn]
  (if conn (stream/close! conn)))

(defn- close-chan! [chan]
  (if chan (async/close! chan)))

(defn stop
  "Performs side effects to shut down the system and release its
  resources. Returns an updated instance of the system."
  [system]
  (let [sys @system]
    (close-chan! (:ticker-chan sys))
    (close-chan! (:fills-chan sys))
    (close-ws-conn! (:ticker-ws-conn sys))
    (close-ws-conn! (:fills-ws-conn sys)))

  (println "Stopping for now")
  (swap! system assoc
         :ticker-chan nil
         :fills-chan nil
         :ticker-ws-conn nil
         :fills-ws-conn nil)
  system)
