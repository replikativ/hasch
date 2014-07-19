# hasch

A library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript, e.g. with SHA-1. Commutative data structures like maps, sets and records are not hashed in order as was the case with e.g. hashing a simple printed edn string, but have the same hash value independent of order. UTF-8 is supported for strings, symbols and keywords.
You can also create UUID5 (using SHA-1) from it. Alternatively you can use your own hash function.

# extension to your own types

Support for edn types on the JVM and JavaScript is complete including records. This works by printing the tagged-literal and rereading it as pure edn, which also ensures that the hashed value can be reproduced beyond the current runtime. Your type has to be pr-str-able for this to work. Records already have a default serialisation.
Hashing performance is dominated by the hashing algorithm (sha-1). Still you can avoid the pr-str/read-string step (effectively allocating double memory) by extending the `IHashCoercion` protocol to your types. You should orient on the `IRecord` implementation and must use `(:literal magics)` to avoid collisions with literal values of the same form. Either by using the default serialisation mechanism to retrieve a hash-value or by extending the hash-coercion, your serialisation (pr-str) or coercion must satisfy the *equality relation*:

- hashes *must* follow `IEquiv` equality of Clojure(Script): `(= a b) <=> (= (edn-hash a) (edn-hash b))`, `(not= a b) <=> (not= (edn-hash a) (edn-hash b))`: Your serialisation has to be *unique*, hashing has to be injective or in other words you might not introduce collisions. Non-equal objects must have non-equal hashes.
- *reflexivity*: `(= (edn-hash a) (edn-hash a))`, including on different runtimes
- *symmetry*: `(= (edn-hash a) (edn-hash b)) <=> (= (edn-hash b) (edn-hash a))` (trivial because of `=`)
- *transitivity*: `(and (= (edn-hash a) (edn-hash b)) (= (edn-hash b) (edn-hash c))) => (= (edn-hash a) (edn-hash c))`
- If you implement *both* pr-str serialisation and hash-coercion, your hash *must* match the default non-extended hash for your (printable) type. In other words implementing `IHashCoercion` must be transparent compared to using the default serialisation. You should implement both printing and coercion if you implement coercion, otherwise you might corrupt hashes later by implementing a different pr-str serialisation.

Getting all that right is not trivial. *Don't mess with hashing extension if you don't have to, just make your type uniquely printable/readable to edn-primitives.*
 
# maturity

The library is tested in cross-platform prototypes, but still very young and alpha-status, don't rely on it for strong consistency yet. The hashing scheme is stable now, but a breaking adjustment might still be necessary. You can stick to a version that works for you though, the code is little and can be maintained with your project.

## Usage

Include in your `project.clj` for Leiningen 2+ with:

~~~clojure
[net.polyc0l0r/hasch "0.2.3"]
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
- 0.2.3 properly dispatch on IRecord (instead of IMap)
- 0.2.2 cannot coerce record tags because of conflicts, rather extend record to properly print
- 0.2.1 fix tag coercion on JVM

# TODO
- Cover serialisation (reading) exceptions for records.
- Nested collections are hashed with the supplied hash-fn before they contribute to the hash-value. This allows to form a peristent data-structure tree by breaking out collection values, so you can rehash top-level collections without pulling the whole value in memory. This is not tested yet, a git-like store could be implemented, e.g. in [geschichte](https://github.com/ghubber/konserve). This should be useful to build durable indexes also. But it might proof to need runtime tweaking, e.g. depending on value size.
- Use test.check/double.check property based tests.
- Profile for performance. 

## License

Copyright © 2014 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
