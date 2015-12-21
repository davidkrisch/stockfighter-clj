# stockfighter-clj

This project is my attempt at Stockfighter in Clojure.  I am a
clojure beginner, so many things are probably wrong/not idiomatic.

To date, the only working API calls are the api and venue
[heartbeats](https://starfighter.readme.io/docs/heartbeat).

## Installation

`git clone https://github.com/davidkrisch/stockfighter-clj.git`

## Usage

Set an environment variable `STARFIGHTER_API_KEY` with your Starfighter
[API key](https://www.stockfighter.io/ui/api_keys).

Then do

```bash
$ lein repl
```

and then

```clojure
=> (require '(stockfighter [client :as c]))
```

Each of the following functions return a map with keys like
`:status` and `:headers` and `:body`.  `body` can be parsed
with

```clojure
=> (c/body (api-heartbeat))
{"ok" true "error" ""}
```

### Stockfighter API Heartbeat

```clojure
=> (c/api-heartbeat)
```

### Exchange (venue) Heartbeat

```clojure
=> (c/venue-heartbeat "TESTEX")
```

### List of an Exchange's Stocks

```clojure
=> (c/stocks-vec (c/stocks "TESTEX"))
[{"name" "Foreign Owned Occluded Bridge Architecture Resources", "symbol" "FOOBAR"}]
```

### Get the orderbook for a stock

```clojure
=> (c/orderbook "TESTEX" "FOOBAR")
```

### Quote for a stock

```clojure
=> (c/stock-quote "TESTEX" "FOOBAR")
```

### Place an order for a stock

```clojure
=> (def request-body (order-body "MYACCOUNT" "TESTEX" "FOOBAR" 10 9999 "buy" "limit"))
=> (c/order "TESTEX" "FOOBAR" request-body)

### Check status on an order

```clojure
=> (def order-id 1234)
=> (c/order-status "TESTEX" "FOOBAR" order-id)
```

### Cancel an order

```clojure
=> (def order-id 1234)
=> (c/cancel-order "TESTEX" "FOOBAR" order-id)
```

## Options

There aren't any yet.

## Examples

```clojure
=> (require '(stockfighter [client :as c]))
nil
=> (c/api-heartbeat)
{:status 200, :headers {"Server" "nginx/1.8.0", "Date" "Sun, 13 Dec 2015 08:32:11 GMT", "Content-Type" "application/json", "Content-Length" "22", "Connection" "close", "Strict-Transport-Security" "max-age=31536000; includeSubdomains"}, :body "{\"ok\":true,\"error\":\"\"}", :request-time 1885, :trace-redirects ["https://api.stockfighter.io/ob/api/heartbeat"], :orig-content-encoding nil}
=> (c/venue-heartbeat "TESTEX")
{:status 200, :headers {"Server" "nginx/1.8.0", "Date" "Sun, 13 Dec 2015 08:34:45 GMT", "Content-Type" "application/json", "Content-Length" "37", "Connection" "close", "Strict-Transport-Security" "max-age=31536000; includeSubdomains"}, :body "{\n  \"ok\": true,\n  \"venue\": \"TESTEX\"\n}", :request-time 1307, :trace-redirects ["https://api.stockfighter.io/ob/api/venues/TESTEX/heartbeat"], :orig-content-encoding nil}
```

### Tests

None that pass!  I'm hoping to get some tests working as I learn more about
clojure and clojure testing/testing-frameworks.

### Bugs

Plenty I'm sure.

## License

Copyright © 2015 David Krisch

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
