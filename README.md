# hasch

A Clojure library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript, e.g. with SHA-1.
You can also create UUID5 (using SHA-1) from it. Alternatively you can use your own hash function.

Support for edn types on the JVM and JavaScript is complete except for tagged literals, which are supposed to create platform specific objects.

REMEMBER THAT BREAKING THE HASH FUNCTION PROBABLY BREAKS YOUR DATA!

You can extend the `IHashCoercion` protocol to your types and use free magic numbers <-111. Please point out flaws you find!

## Usage

FIXME

## License

Copyright © 2014 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
