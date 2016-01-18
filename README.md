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

## Options

There aren't any yet.

## Examples

See API docs above.

### Tests

None that pass!  I'm hoping to get some tests working as I learn more about
clojure and clojure testing/testing-frameworks.

### Bugs

Plenty I'm sure.

## License

Copyright © 2015 David Krisch

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
