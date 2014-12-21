# hasch

*A new optimized hashing scheme is coming up with version 0.3.0, see below.*

A library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript, e.g. with SHA-1. Commutative data structures like maps, sets and records are not hashed in order as was the case with e.g. hashing a simple printed edn string, but have the same hash value independent of order (read: always and everywhere). UTF-8 is supported for strings, symbols and keywords.
You can also create UUID5 (using SHA-1) from it. Alternatively you can use your own hash function.

Support for edn types on the JVM and JavaScript is complete including records. This works by printing the tagged-literal and rereading it as pure edn, which also ensures that the hashed value can be reproduced beyond the current runtime. Your type has to be pr-str-able for this to work. Records already have a default serialisation.

## Maturity

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

# Upcoming stable version 0.3.0

A library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript with SHA-512. The main motivation is that commutative data structures like maps, sets and records are not hashed in order as was the case with e.g. hashing a simple sequential serialisation, but have the same hash value independent of order. That way Clojure value semantics with `edn` are retained. UTF-8 is supported for strings, symbols and keywords. Beyong this tagged literals are supported in a generic runtime independant fashion and platform-neutral encoding (atm. between JVM and JavaScript) is taken care of.
You can then create UUID5 (using SHA-512) from it. Alternatively you can use your own hash function, but this is not standardized and hence beyond the spec.

## Motivation

The motivation is to exchange (potentially large) values in a hostile environment without conflicts. The concrete design motivation is to use the commit log of [geschichte](https://github.com/ghubber/geschichte) for exchange of datascript/datomic transaction logs. As long as you are in a trusted environment you can trust the random generator for conflict-free UUIDs as is done internally by many Clojure project's, but as soon as you distribute values, collisions can happen. Note that you can treat hasch's cryptographic UUIDs like random UUIDs internally and don't need to verify them.

## Why not use Clojure's `hash`?

I wish I could have done that instead of reimplementing my own hashing scheme for edn (there are more interesting problems). There is one major reason against using internal hash functions: They need to be very fast for efficient data-structures and hence trade this for potential but unlike collisions, which is unacceptable in an unsecure environment. For the same reason they also only work on 64 bit values, which is fine for a runtime, but not the internet.

## edn support

Support for edn types on the JVM (but not yet JavaScript) is complete including records. This works by printing the tagged-literal and rereading it as pure edn, which also ensures that the hashed value can be reproduced beyond the current runtime. Your type has to be pr-str-able for this to work. Records already have a default serialisation.


## Safety

The library is designed safety first, speed second. I have put quite some thought into getting all input bits (entropy) into the cryptographic hash function. It should be impossible to construct a collision (beyond weaknesses in the underlying SHA-512 which is considered safe in year 2014). The biggest conceptual weakness is XOR-ing of sha-512 hashed elements in maps and sets.

*Once released, I'll offer a 100 $ bounty for proof of any collision, just open a github issue. For implementation bugs, I'll send you special sweets from Mannheim. :-)*

## Speed

The first versions were just build around safety, but perform poorly with large values. The speed should be sufficient to be in the same order of magnitude as transmission speed (throughput + latency) over slow to mid-range internet broadband connections. If you want to transmit larger values fast, you maybe can chose a sequential binary encoding.

*These are just micro-benchmarks on my 3 year laptop (old version is 4-10x slower), I just mention them so you can get an impression.*

~~~clojure
;; most important and worst case, what can be done?
hasch.platform> (let [val (into {} (doall (map vec (partition 2
                                                (interleave (range 1000000)
                                                            (range 1000000 2000000))))))]
    (bench (-coerce val (sha512-message-digest) sha512-message-digest)))
Evaluation count : 60 in 60 samples of 1 calls.
             Execution time mean : 5.691272 sec
    Execution time std-deviation : 194.356782 ms
   Execution time lower quantile : 5.572440 sec ( 2.5%)
   Execution time upper quantile : 6.304907 sec (97.5%)
                   Overhead used : 1.967460 ns

Found 4 outliers in 60 samples (6.6667 %)
	low-severe	 1 (1.6667 %)
	low-mild	 3 (5.0000 %)
 Variance from outliers : 20.6184 % Variance is moderately inflated by outliers
nil
hasch.platform> (let [val (doall (range 1000000))]
    (bench (-coerce val (sha512-message-digest) sha512-message-digest)))
Evaluation count : 240 in 60 samples of 4 calls.
             Execution time mean : 269.192864 ms
    Execution time std-deviation : 13.568064 ms
   Execution time lower quantile : 264.196463 ms ( 2.5%)
   Execution time upper quantile : 290.258534 ms (97.5%)
                   Overhead used : 1.967460 ns

Found 8 outliers in 60 samples (13.3333 %)
	low-severe	 4 (6.6667 %)
	low-mild	 4 (6.6667 %)
 Variance from outliers : 36.8270 % Variance is moderately inflated by outliers
nil
hasch.platform> (let [val (doall (into #{} (range 1000000)))]
    (bench (-coerce val (sha512-message-digest) sha512-message-digest)))
Evaluation count : 60 in 60 samples of 1 calls.
             Execution time mean : 2.307726 sec
    Execution time std-deviation : 233.571036 ms
   Execution time lower quantile : 2.171922 sec ( 2.5%)
   Execution time upper quantile : 3.105212 sec (97.5%)
                   Overhead used : 1.967460 ns

Found 9 outliers in 60 samples (15.0000 %)
	low-severe	 3 (5.0000 %)
	low-mild	 6 (10.0000 %)
 Variance from outliers : 70.3610 % Variance is severely inflated by outliers
nil
hasch.platform> (let [val (doall (repeat 1000000 "hello"))]
    (bench (-coerce val (sha512-message-digest) sha512-message-digest)))
Evaluation count : 180 in 60 samples of 3 calls.
             Execution time mean : 430.994775 ms
    Execution time std-deviation : 85.018651 ms
   Execution time lower quantile : 383.670949 ms ( 2.5%)
   Execution time upper quantile : 652.204588 ms (97.5%)
                   Overhead used : 1.967460 ns

Found 11 outliers in 60 samples (18.3333 %)
	low-severe	 11 (18.3333 %)
 Variance from outliers : 91.1043 % Variance is severely inflated by outliers
nil
hasch.platform> (let [val (doall (repeat 1000000 :foo/bar))]
    (bench (-coerce val (sha512-message-digest) sha512-message-digest)))
WARNING: Final GC required 1.0538668933601911 % of runtime
Evaluation count : 60 in 60 samples of 1 calls.
             Execution time mean : 967.548623 ms
    Execution time std-deviation : 245.052748 ms
   Execution time lower quantile : 637.265154 ms ( 2.5%)
   Execution time upper quantile : 1.273569 sec (97.5%)
                   Overhead used : 1.967460 ns
nil
hasch.platform> (let [val (byte-array (* 1024 1024 300) (byte 42))] ;; 300 mib bytearray
    (bench (-coerce val (sha512-message-digest) sha512-message-digest)))
Evaluation count : 60 in 60 samples of 1 calls.
             Execution time mean : 1.987549 sec
    Execution time std-deviation : 134.189868 ms
   Execution time lower quantile : 1.901676 sec ( 2.5%)
   Execution time upper quantile : 2.304744 sec (97.5%)
                   Overhead used : 1.967460 ns

Found 3 outliers in 60 samples (5.0000 %)
	low-severe	 3 (5.0000 %)
 Variance from outliers : 50.1416 % Variance is severely inflated by outliers
nil

~~~


# Changes
- 0.2.3 properly dispatch on IRecord (instead of IMap)
- 0.2.2 cannot coerce record tags because of conflicts, rather extend record to properly print
- 0.2.1 fix tag coercion on JVM

## Extension to your own types

*Warning*: Getting all that right is not trivial. Don't mess with hashing extension if you don't have to, just make your type uniquely printable/readable to edn-primitives!

You can avoid the pr-str/read-string step (also effectively allocating double memory) by extending the `IHashCoercion` protocol to your types. You should orient on the `IRecord` implementation and must use `(:literal magics)` to avoid collisions with literal values of the same form. Either by using the default serialisation mechanism to retrieve a hash-value or by extending the hash-coercion, your serialisation (pr-str) or coercion must satisfy the *equality relation*:

- hashes *must* follow `IEquiv` equality of Clojure(Script): `(= a b) <=> (= (edn-hash a) (edn-hash b))`, `(not= a b) <=> (not= (edn-hash a) (edn-hash b))`: Your serialisation has to be *unique*, hashing has to be injective or in other words you might not introduce collisions. Non-equal objects must have non-equal hashes.
- *reflexivity*: `(= (edn-hash a) (edn-hash a))`, including on different runtimes
- *symmetry*: `(= (edn-hash a) (edn-hash b)) <=> (= (edn-hash b) (edn-hash a))` (trivial because of `=`)
- *transitivity*: `(and (= (edn-hash a) (edn-hash b)) (= (edn-hash b) (edn-hash c))) => (= (edn-hash a) (edn-hash c))`
- If you implement *both* pr-str serialisation and hash-coercion, your hash *must* match the default non-extended hash for your (printable) type. In other words implementing `IHashCoercion` must be transparent compared to using the default serialisation. You should implement both printing and coercion if you implement coercion, otherwise you might corrupt hashes later by implementing a different pr-str serialisation.


# TODO
- Cover serialisation (reading) exceptions for records.
- Nested collections are hashed with the supplied hash-fn before they contribute to the hash-value. This allows to form a peristent data-structure tree by breaking out collection values, so you can rehash top-level collections without pulling the whole value in memory. This is not tested yet, a git-like store could be implemented, e.g. in [geschichte](https://github.com/ghubber/konserve). This should be useful to build durable indexes also. But it might proof to need runtime tweaking, e.g. depending on value size.
- Use test.check/double.check property based tests.
- Profile for performance.

## License

Copyright © 2014 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
