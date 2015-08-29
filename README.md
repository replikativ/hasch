# hasch

*A new optimized hashing scheme is coming up with version 0.3.0, see below.*

A library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript, e.g. with SHA-1. Commutative data structures like maps, sets and records are not hashed in order as was the case with e.g. hashing a simple printed edn string, but have the same hash value independant of order (read: always and everywhere). UTF-8 is supported for strings, symbols and keywords.
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

# Upcoming version 0.3.0 (will be stable hashing scheme)

Add this to your leiningen project's dependencies:
[![Clojars Project](http://clojars.org/es.topiq/hasch/latest-version.svg)](http://clojars.org/es.topiq/hasch)


A library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript with SHA-512. The main motivation is that commutative data structures like maps, sets and records are not hashed in order as was the case with e.g. hashing a simple sequential serialisation, but have the same hash value independant of order. That way Clojure value semantics with `edn` are retained. UTF-8 is supported for strings, symbols and keywords. Beyond this tagged literals are supported in a generic runtime independant fashion and platform-neutral encoding (atm. between JVM and JavaScript) is taken care of.
You can then create UUID5 (using SHA-512) from it. Alternatively you can use your own hash function, but this is not standardized and hence beyond the spec.

## Motivation

The motivation is to exchange (potentially large) values in a hostile environment without conflicts. The concrete design motivation is to use the commit log of [geschichte](https://github.com/ghubber/geschichte) for exchange of datascript/datomic transaction logs. As long as you are in a trusted environment you can trust the random generator for conflict-free UUIDs as is done internally by many Clojure projects, but as soon as you distribute values, collisions can happen. Note that you can treat hasch's cryptographic UUIDs like random UUIDs internally and don't need to verify them.

## Why not use Clojure's `hash`?

I wish I could have done that instead of reimplementing my own hashing scheme for edn (there are more interesting problems). There is one major reason against using internal hash functions: They need to be very fast for efficient data-structures and hence trade this for potential but unlike collisions, which is unacceptable in an unsecure environment. For the same reason they also only work on 64 bit values, which is fine for a runtime, but not the internet.

## Why not sort?

Sorting of heterogenous collections requires a unique serialization (e.g. pr-str or our encoding) on keys beforehand, which was sadly not faster even for small maps and sets. Sorting on number only maps was faster for maps until at least a size of one million. At some point the complexity of sorting becomes more expansive than xor-ing hashed kv-vectors, so sorting is a simple but not linearly scalable solution. Still it could prove valuable in the future.

## edn support

Support for edn types on the JVM (but not yet JavaScript) is complete including records. This works by printing the tagged-literal and rereading it as pure edn, which also ensures that the hashed value can be reproduced beyond the current runtime. Your type has to be pr-str-able for this to work. Records already have a default serialisation.


## Safety

The library is designed safety first, speed second. I have put quite some thought into getting all input bits (entropy) into the cryptographic hash function. It should be impossible to construct a collision (beyond weaknesses in the underlying SHA-512 which is considered safe in year 2014). The biggest conceptual weakness is XOR-ing of sha-512 hashed elements in maps and sets.

*Once released, I'll offer a 100 $ bounty for proof of any collision, just open a github issue. This hashing is an important building block for distributed systems to me.*

## Speed

The first versions were just build around safety, but perform poorly with large values. The speed should be sufficient to be in the same order of magnitude as transmission speed (throughput + latency) over slow to mid-range internet broadband connections. If you want to transmit larger values fast, you maybe can chose a sequential binary encoding with native hashing speed. JavaScript performance is still significantly slower (~10x), seemingly due to the lack of native SHA hashing routines.

*These are just micro-benchmarks on my 3 year old laptop, I just mention them so you can get an impression. *

~~~clojure
;; most important and worst case, what can be done?
hasch.platform> (let [val (into {} (doall (map vec (partition 2
                                                (interleave (range 1000000)
                                                            (range 1000000))))))]
    (bench (-coerce val sha512-message-digest)))
Evaluation count : 60 in 60 samples of 1 calls.
             Execution time mean : 3.596037 sec
    Execution time std-deviation : 23.812536 ms
   Execution time lower quantile : 3.566430 sec ( 2.5%)
   Execution time upper quantile : 3.647540 sec (97.5%)
                   Overhead used : 2.039920 ns

Found 4 outliers in 60 samples (6.6667 %)
	low-severe	 3 (5.0000 %)
	low-mild	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil

hasch.platform> (let [val (doall (range 1000000))]
    (bench (-coerce val sha512-message-digest)))
Evaluation count : 240 in 60 samples of 4 calls.
             Execution time mean : 297.320276 ms
    Execution time std-deviation : 2.683060 ms
   Execution time lower quantile : 293.217179 ms ( 2.5%)
   Execution time upper quantile : 302.059975 ms (97.5%)
                   Overhead used : 2.039920 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil

hasch.platform> (let [val (doall (into #{} (range 1000000)))]
    (bench (-coerce val sha512-message-digest)))
Evaluation count : 60 in 60 samples of 1 calls.
             Execution time mean : 2.733429 sec
    Execution time std-deviation : 15.463782 ms
   Execution time lower quantile : 2.708645 sec ( 2.5%)
   Execution time upper quantile : 2.758701 sec (97.5%)
                   Overhead used : 2.039920 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil

hasch.platform> (let [val (doall (repeat 1000000 "hello world"))]
    (bench (-coerce val sha512-message-digest)))
WARNING: Final GC required 1.472161970438994 % of runtime
Evaluation count : 120 in 60 samples of 2 calls.
             Execution time mean : 873.084789 ms
    Execution time std-deviation : 5.753430 ms
   Execution time lower quantile : 862.909606 ms ( 2.5%)
   Execution time upper quantile : 885.560937 ms (97.5%)
                   Overhead used : 2.039920 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil

hasch.platform> (let [val (doall (repeat 1000000 :foo/bar))]
    (bench (-coerce val sha512-message-digest)))
WARNING: Final GC required 1.072577784478402 % of runtime
Evaluation count : 120 in 60 samples of 2 calls.
             Execution time mean : 756.394263 ms
    Execution time std-deviation : 2.935836 ms
   Execution time lower quantile : 750.827152 ms ( 2.5%)
   Execution time upper quantile : 761.299697 ms (97.5%)
                   Overhead used : 2.039920 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil

hasch.platform> (let [val (byte-array (* 1024 1024 300) (byte 42))] ;; 300 mib bytearray
    (bench (-coerce val sha512-message-digest)))
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

hasch.platform> (let [val (doall (vec (repeat 10000 {:db/id 18239
                                       :person/name "Frederic"
                                       :person/familyname "Johanson"
                                       :person/street "Fifty-First Street 53"
                                       :person/postal 38237
                                       :person/phone "02343248474"
                                       :person/weight 38.23})))]
    (bench (-coerce val sha512-message-digest)))
WARNING: Final GC required 1.2237845534164749 % of runtime
Evaluation count : 240 in 60 samples of 4 calls.
             Execution time mean : 322.164678 ms
    Execution time std-deviation : 1.821136 ms
   Execution time lower quantile : 318.232462 ms ( 2.5%)
   Execution time upper quantile : 325.916354 ms (97.5%)
                   Overhead used : 2.039920 ns

Found 4 outliers in 60 samples (6.6667 %)
	low-severe	 2 (3.3333 %)
	low-mild	 1 (1.6667 %)
	high-mild	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil

~~~


# Changes
- 0.3.0 Overhaul encoding for ~10x-20x times performance on the JVM. Use safe SHA-512. Add byte-array support for blobs.
- 0.2.3 properly dispatch on IRecord (instead of IMap)
- 0.2.2 cannot coerce record tags because of conflicts, rather extend record to properly print
- 0.2.1 fix tag coercion on JVM

## Extension to your own types

*Warning*: Getting all that right is not trivial. Don't mess with hashing extension if you don't have to, just make your type uniquely printable/readable to edn-primitives!

You can avoid the pr-str/read-string step (also effectively allocating double memory) by extending the `IHashCoercion` protocol to your types. You should orient on the `IRecord` implementation and must use `(:literal magics)` to avoid collisions with literal values of the same form. Either by using the default serialisation mechanism to retrieve a hash-value or by extending the hash-coercion, your serialisation (pr-str) or coercion must satisfy the *equality relation*:

- hashes *must* follow `IEquiv` equality of Clojure(Script): `(= a b) <=> (= (edn-hash a) (edn-hash b))`, `(not= a b) <=> (not= (edn-hash a) (edn-hash b))`: Your serialisation has to be *unique*, hashing has to be injective or in other words you might not introduce collisions. Non-equal objects must have non-equal hashes.
- *reflexivity*: `(= (edn-hash a) (edn-hash a))`, including on different runtimes
- *symmetry*: `(= (edn-hash a) (edn-hash b)) <=> (= (edn-hash b) (edn-hash a))` (trivial because of `=`)
- *transitivity*: `(and (= (edn-hash a) (edn-hash b)) (= (edn-hash b) (edn-hash c))) => (= (edn-hash a) (edn-hash c))` (also trivial because of `=`)
- If you implement *both* pr-str serialisation and hash-coercion, your hash *must* match the default non-extended hash for your (printable) type. In other words implementing `IHashCoercion` must be transparent compared to using the default serialisation. You should implement both printing and coercion if you implement coercion, otherwise you might corrupt hashes later by accidentally creating a different pr-str serialisation.


# TODO
- Ensure grounded tagged literals are hashed the same as records
- Use test.check/double.check property based tests between Java and JS (?)
- Nested collections are hashed with the supplied hash-fn before they contribute to the hash-value. This allows to form a peristent data-structure tree by breaking out collection values, so you can rehash top-level collections without pulling the whole value in memory. This is not tested yet, a git-like store could be implemented, e.g. in [geschichte](https://github.com/ghubber/konserve). This should be useful to build durable indexes also. But it might proof to need runtime tweaking, e.g. depending on value size.
- If keeping sorted maps/sets is feasable for high-throughput applications, allow to hash them sequentally.

## License

Copyright © 2014-2015 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
