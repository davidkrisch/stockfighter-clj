# stockfighter-clj

This project is my attempt at Stockfighter in Clojure.  I am a
Clojure beginner, so many things are probably wrong or not idiomatic.

Code reviews and pull requests are *most* appreciated.

## Installation

1. Install leiningen
2. `git clone https://github.com/davidkrisch/stockfighter-clj.git`

## Usage

Set an environment variable `STARFIGHTER_API_KEY` with your Stockfighter
[API key](https://www.stockfighter.io/ui/api_keys).

Then start a repl

```bash
$ lein repl
```

Functions to interact with the Stockfighter API are loaded into the
`user` namespace at startup.

```clojure
user=> (body (api-heartbeat))
{"ok" true "error" ""}
user=> (:status (stock-quote "TESTEX" "FOOBAR"))
200
user=> (-> (venue-heartbeat "TESTEX") body (get "ok"))
```

## Stockfighter API

Each of the following functions return a map with keys like
`:status` and `:headers` and `:body`.

### Stockfighter API Heartbeat

```clojure
=> (api-heartbeat)
```

### Exchange (venue) Heartbeat

```clojure
=> (venue-heartbeat "TESTEX")
```

### List of an Exchange's Stocks

```clojure
=> (stocks "TESTEX")
```

### Get the orderbook for a stock

```clojure
=> (orderbook "TESTEX" "FOOBAR")
```

### Quote for a stock

```clojure
=> (stock-quote "TESTEX" "FOOBAR")
```

### Place an order for a stock

See the [official documentation](https://starfighter.readme.io/docs/place-new-order).

```clojure
=> (def request-body (order-body "EXB123456" ;; Account number
                                 "TESTEX"    ;; Venue
                                 "FOOBAR"    ;; Stock symbol
                                 10          ;; quantity
                                 9999        ;; price $99.99
                                 "buy"       ;; direction - "buy" or "sell"
                                 "limit"))   ;; order type
=> (order "TESTEX" "FOOBAR" request-body)
```

### Check status on an order

```clojure
=> (def order-id 1234)
=> (order-status "TESTEX" "FOOBAR" order-id)
```

### Cancel an order

```clojure
=> (def order-id 1234)
=> (cancel-order "TESTEX" "FOOBAR" order-id)
```

### Ticker Tape

The methods above are request/response oriented.  `ticker`
connects to a websocket and returns a stream of stock
quotes.   This is done in a separate thread so it doesn't
tie up the repl while it is connected

```clojure
=> (def system {:chan-out (chan (sliding/buffer 1))
                :account "EXB123456"
                :venue "TESTEX"
                :stock "FOOBAR"
                :qty 10
                :order-type "limit"
                })
=> (ticker system)
```

Message received on the ticker tape websocket will be placed
on the `:chan-out` channel and can be retrieved with a blocking
call like:

```clojure
=> (<!! (:chan-out system))
```

Closing the websocket is accomplished by closing `:chan-out`

```clojure
=> (close! (:chan-out system))
```

### Options

There aren't any yet, but it would be nice to be able to turn on debugging
for HTTP requests and maybe other parts of the system.

## Design

### Data Model

In the `dev.user` namespace there is a top level `atom` called __system__.
It is a `map` that holds the application's state and configuration.

#### State

The state of the application is held in `(:trades system)`.  It is an unordered
vector of __trade__ maps.  Each trade represents a single order.  The trade
could be in any state: failed, unfilled, partially filled, or fully filled.

```clojure
(def system
  :account "<account-id>"
  :venue "<venue-id>"
  :stock "<stock-symbol>"
  ...
  <other-stuff>
  ...
  :trades [{:order-body {:account account
                         :venue venue
                         :stock stock
                         :qty qty
                         :price price
                         :direction direction
                         :orderType orderType}
             :response (manifold/deferred)
             :ts <timestamp>
           }
           ...])

```

<dl>
  <dt>:order-body</dt>
    <dd>(map) The POST body we submitted with the order</dd>

  <dt>:response</dt>
    <dd>(manifold/deferred) The response, possibly not realized.</dd>

  <dt>:ts</dt>
    <dd>Timestamp - when the order was submitted.</dd>
</dl>

#### Navigating :trades

`:trades` contains every interaction we've had with the system, so we can
answer many questions with it:

1. What does my inventory look like? (long or short)
2. What is the averge buy/sell price?
3. What is my profit?
4. How many unfilled orders do I have and how many shares are involved?
5. How will my unfilled orders effect my inventory?

These questions can be answered using the functions in the mostly incomplete
`stockfighter.state` namespace.  I'm workin' in it!

### Processes

There are 5 processes

- Ticker - connects to the quote ticker websocket and puts messages on a channel.
  Runs in its own thread until `async/channel` is closed.
- Order - Place orders using quotes from the ticker.
- Inventory Maintenance - Its job is to minimize inventory. Single thread running in a loop
  possibly with some sort of delay.
- Order Status - manage orders once they've been placed.  Thread per order using
  a dedicated thread pool
- Visibility - periodically wakes up and prints state of system.

## License

Copyright © 2015 David Krisch

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
