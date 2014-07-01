# hasch

A library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript, e.g. with SHA-1. Commutative data structures like maps, sets and records are not hashed in order as was the case with e.g. hashing a simple printed edn string, but have the same hash value independent of order. UTF-8 is supported for strings, symbols and keywords.
You can also create UUID5 (using SHA-1) from it. Alternatively you can use your own hash function.

Support for edn types on the JVM and JavaScript is complete including records. The library is tested in cross-platform prototypes, but still very young and alpha-status, don't rely on it for strong consistency yet. The hashing scheme is stable now, but a breaking adjustment might still be necessary.

## Usage

Include in your `project.clj` for Leiningen 2+ with:

~~~clojure
[net.polyc0l0r/hasch "0.2.2"]
~~~

Then you can access the major function through `hasch.core`:

~~~clojure
(use 'hasch.core)
(edn-hash ["hello world" {:a 3.14} #{42} '(if true nil \f)])
=> (-104 -11 -108 -93 -66 119 -92 73 32 -75 -73 21 82 -31 -45 122 -31 115 27 -23)

(uuid5 (edn-hash "hello world"))
=> #uuid "09c1649c-40c3-51cc-829c-dc781de2eda0"

;; or just use the convenience multi-arity uuid fn:
(uuid) => #uuid "a27dfbb9-b69a-4f08-8df4-471464bfeb37"
(uuid "hello world") => #uuid "09c1649c-40c3-51cc-829c-dc781de2eda0"
~~~

# Changes
- 0.2.2 cannot coerce record tags because of conflicts, rather extend record to properly print
- 0.2.1 fix tag coercion on JVM

# TODO
- Automate tests in js (with cljx?).
- Nested collections are hashed with the supplied hash-fn before they contribute to the hash-value. This allows to form a peristent data-structure tree by breaking out collection values, so you can rehash top-level collections without pulling the whole value in memory. This is not tested yet, a git-like store could be implemented, e.g. in [geschichte](https://github.com/ghubber/konserve). This should be useful to build durable indexes also. But it might proof to need runtime tweaking, e.g. depending on value size.
- Use test.check/double.check property based tests.
- Profile for performance. Maybe avoid full pr-str/read-string and only apply to records...

## License

Copyright © 2014 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
