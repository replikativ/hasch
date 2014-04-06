# hasch

A Clojure library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript, e.g. with SHA-1.
You can also create UUID5 (using SHA-1) from it. Alternatively you can use your own hash function.

Support for edn types on the JVM and JavaScript is complete except for tagged literals, which are supposed to create platform specific objects and cannot be supported.

REMEMBER THAT BREAKING THE HASH FUNCTION PROBABLY BREAKS YOUR DATA!

You can extend the `IHashCoercion` protocol to your types and use free magic numbers -128<x<-111.

*Please point out flaws you find!*

# TODO
- Automate tests from js (with cljx?).
- Use test.check/double.check property based tests.
- Profile for performance

## Usage

    (use 'hasch.core)
    (edn-hash ["hello world" {:a 3.14} #{42} '(if true nil \f)])
    => (-104 -11 -108 -93 -66 119 -92 73 32 -75 -73 21 82 -31 -45 122 -31 115 27 -23)

    (uuid5 (edn-hash "hello world"))
    => #uuid "09c1649c-40c3-51cc-829c-dc781de2eda0"

## License

Copyright © 2014 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
