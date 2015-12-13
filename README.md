# stockfighter-clj

This project is my attempt at Stockfighter in Clojure.  I am a
clojure beginner, so many things are probably wrong/not idiomatic.

To date, the only working API calls are the api and venue
[heartbeats](https://starfighter.readme.io/docs/heartbeat).

## Installation

`git clone https://github.com/davidkrisch/stockfighter-clj.git`

## Usage

Set an environment variable with your Starfighter
[API key](https://www.stockfighter.io/ui/api_keys).

The environment variable should be called `STARFIGHTER_API_KEY`

The do `lein repl` and then

```clojure
(require '(stockfighter [client :as c]))
(api-heartbeat)
```

will check if the Stockfighter API is up.

## Options

There aren't any yet.

## Examples

```clojure
(require '(stockfighter [client :as c]))
(api-heartbeat)
(venue-heartbeat "TESTEX")
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
