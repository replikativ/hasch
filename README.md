# hasch

A Clojure library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript, e.g. with SHA-1.
You can also create UUID5 (using SHA-1) from it. Alternatively you can use your own hash function.

Support for edn types on the JVM and JavaScript is complete except for tagged literals, which are supposed to create platform specific objects and cannot be supported.

REMEMBER THAT BREAKING THE HASH FUNCTION PROBABLY BREAKS YOUR DATA!

You can extend the `IHashCoercion` protocol to your types and use free magic numbers -128<x<-111.

*Please point out flaws you find!*

# TODO
- Use test.check/double.check property based tests.
- Profile for performance

## Usage

    (use 'hasch.core)
    (edn-hash ["hello world" {:a 3.14} #{42} '(if true nil \f)])
    => (-34 50 4 7 -55 3 90 33 -124 27 12 68 -85 -12 29 -79 100 96 -27 118)

    (use 'hasch.platform)
    (uuid5 (edn-hash "hello world"))
    => #uuid "32860372-c8c5-5b05-96bc-2ced270f305b"

## License

Copyright © 2014 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
