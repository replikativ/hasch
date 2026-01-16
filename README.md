# hasch


<p align="center">
<a href="https://clojurians.slack.com/archives/CB7GJAN0L"><img src="https://img.shields.io/badge/clojurians%20slack-join%20channel-blueviolet"/></a>
<a href="https://clojars.org/org.replikativ/hasch"> <img src="https://img.shields.io/clojars/v/org.replikativ/hasch.svg" /></a>
<a href="https://circleci.com/gh/replikativ/hasch"><img src="https://circleci.com/gh/replikativ/hasch.svg?style=shield"/></a>
<a href="https://github.com/replikativ/hasch/tree"><img src="https://img.shields.io/github/last-commit/replikativ/hasch"/></a>
<a href="https://versions.deps.co/replikativ/hasch" title="Dependencies Status"><img src="https://versions.deps.co/replikativ/hasch/status.svg" /></a>
</p>

A library to consistently crypto-hash [edn](https://github.com/edn-format/edn) data structures on Clojure and ClojureScript with SHA-512. The main motivation is that commutative data structures like maps, sets and records are not hashed in order as was the case with e.g. hashing a simple sequential serialisation, but have the same hash value independent of order. That way Clojure value semantics with `edn` are retained. UTF-8 is supported for strings, symbols and keywords. Beyond this tagged literals are supported in a generic runtime independent fashion and platform-neutral encoding (atm. between JVM and JavaScript) is taken care of.
You can then create UUID5 (using SHA-512) from it. Alternatively you can use your own hash function, but this is not standardized and hence beyond the spec.

Support for edn types on the JVM and JavaScript is complete including records. This works by printing the tagged-literal and rereading it as pure edn, which also ensures that the hashed value can be reproduced beyond the current runtime. Your type has to be pr-str-able for this to work. Records already have a default serialisation.

## Usage <a href="https://gitter.im/replikativ/replikativ?utm_source=badge&amp;utm_medium=badge&amp;utm_campaign=pr-badge&amp;utm_content=badge"><img src="https://camo.githubusercontent.com/da2edb525cde1455a622c58c0effc3a90b9a181c/68747470733a2f2f6261646765732e6769747465722e696d2f4a6f696e253230436861742e737667" alt="Gitter" data-canonical-src="https://badges.gitter.im/Join%20Chat.svg" style="max-width:100%;"></a>


Add this to your leiningen project's dependencies:
[![Clojars Project](http://clojars.org/org.replikativ/hasch/latest-version.svg)](http://clojars.org/org.replikativ/hasch)

Then you can access the major function through `hasch.core`:

~~~clojure
(use 'hasch.core)
(edn-hash ["hello world" {:a 3.14} #{42} '(if true nil \f)])
=> (120 75 53 36 42 91 14 22 174 251 7 222 83 57 158 140 192 131 251 17 176 29 252 118 83 2 106 187 223 17 84 232 24 103 183 27 19 174 222 37 246 138 132 126 172 46 249 42 62 46 66 32 33 100 88 168 4 242 90 25 5 228 2 88)

(uuid5 (edn-hash "hello world"))
=> #uuid "1227fe0a-471b-5329-88db-875fb82737a8"

;; or just use the convenience multi-arity uuid fn:
(uuid) => #uuid "a27dfbb9-b69a-4f08-8df4-471464bfeb37"
(uuid "hello world") => #uuid "1227fe0a-471b-5329-88db-875fb82737a8"
~~~


## Motivation

The motivation is to exchange (potentially large) values in a hostile environment without conflicts. The concrete design motivation is to use the commit log of [replikativ](https://github.com/replikativ/replikativ) for exchange of datascript/datomic transaction logs. As long as you are in a trusted environment you can trust the random generator for conflict-free UUIDs as is done internally by many Clojure projects, but as soon as you distribute values, collisions can happen. Note that you can treat hasch's cryptographic UUIDs like random UUIDs internally and don't need to verify them.

## Maturity

The library is tested in cross-platform [applications](https://github.com/replikativ/topiq). The hashing scheme can be considered stable. It is versioned, so we can fix any severe bug without breaking stored hashes.


## Why not use Clojure's `hash`?

I wish I could have done that instead of reimplementing my own hashing scheme for edn (there are more interesting problems). There is one major reason against using internal hash functions: They need to be very fast for efficient data-structures and hence trade this for potential but unlike collisions, which is unacceptable in an unsecure environment. For the same reason they also only work on 64 bit values, which is fine for a runtime, but not the internet.

## Why not sort?

Sorting of heterogenous collections requires a unique serialization (e.g. pr-str or our encoding) on keys beforehand, which was sadly not faster even for small maps and sets. Sorting on number only maps was faster for maps until at least a size of one million. At some point the complexity of sorting becomes more expansive than xor-ing hashed kv-vectors, so sorting is a simple but not linearly scalable solution. Still it could prove valuable in the future.

## edn support

Support for `edn` types is complete including records. This works according to [incognito](https://github.com/replikativ/incognito) by hashing unknown records the same as their known counterparts. You need to supply the optional `write-handlers` to `uuid` if your records have a custom serialization. Otherwise incognito records won't match.
Importantly the JVM class names are converted into cljs format `foo.bar_baz.Bar` -> `foo.bar-baz/Bar` before hashing. While this potentially allows maliciously induced collisions, you are safe if you use `incognito` or a similar mapping for cross-platform support, as it automatically serializes all record tags accordingly.

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
- 0.3.5 Support BigInteger and BigDecimal hashing (same as for limited precision types).
- 0.3.4 Expose high-level base64 hashes with full precision.
- 0.3.2 Minimize dependencies, explicit profiles for different Clojure(Script) versions
- 0.3.1 fix bug in hashing sequences containing null
- 0.3.0 fix accidental hashing of records as maps
- 0.3.0-beta4 fix record serialization with incognito
- 0.3.0 Overhaul encoding for ~10x-20x times performance on the JVM. Use safe SHA-512. Add byte-array support for blobs.
- 0.2.3 properly dispatch on IRecord (instead of IMap)
- 0.2.2 cannot coerce record tags because of conflicts, rather extend record to properly print
- 0.2.1 fix tag coercion on JVM

## Extension to your own types

*Warning*: Getting all that right is not trivial. Don't mess with hashing extension if you don't have to, just make your type uniquely mappable with [incognito](https://github.com/replikativ/incognito)!

You can avoid the mapping step to Clojure datastructures (also effectively allocating double memory) by extending the `hasch.benc/PHashCoercion` protocol to your types. You should orient on the `IRecord` implementation and must use `(:literal magics)` to avoid collisions with literal values of the same form. Either by using the default serialisation mechanism to retrieve a hash-value or by extending the hash-coercion, your serialisation or coercion must satisfy the *equality relation*:

- hashes *must* follow `IEquiv` equality of Clojure(Script): `(= a b) <=> (= (edn-hash a) (edn-hash b))`, `(not= a b) <=> (not= (edn-hash a) (edn-hash b))`: Your serialisation has to be *unique*, hashing has to be injective or in other words you might not introduce collisions. Non-equal objects must have non-equal hashes.
- *reflexivity*: `(= (edn-hash a) (edn-hash a))`, including on different runtimes
- *symmetry*: `(= (edn-hash a) (edn-hash b)) <=> (= (edn-hash b) (edn-hash a))` (trivial because of `=`)
- *transitivity*: `(and (= (edn-hash a) (edn-hash b)) (= (edn-hash b) (edn-hash c))) => (= (edn-hash a) (edn-hash c))` (also trivial because of `=`)


# TODO
- Use test.check/double.check property based tests between Java and JS (?)
- Nested collections are hashed with the supplied hash-fn before they contribute to the hash-value. This allows to form a Merkle-tree like peristent data-structure by breaking out collection values, so you can rehash top-level collections without pulling the whole value in memory. This is not tested yet, a git-like store could be implemented, e.g. in [konserve](https://github.com/replikativ/konserve). This should be useful to build durable indexes also. But it might proof to need runtime tweaking, e.g. depending on value size.
- If keeping sorted maps/sets is feasable for high-throughput applications, allow to hash them sequentally.

# Contributors
- Max Penet
- James Conroy-Finn
- Konrad Kühne
- Christian Weilbach

## License

Copyright © 2014-2018 Christian Weilbach and contributors

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
