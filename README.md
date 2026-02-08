# hasch


<p align="center">
<a href="https://clojurians.slack.com/archives/CB7GJAN0L"><img src="https://img.shields.io/badge/clojurians%20slack-join%20channel-blueviolet"/></a>
<a href="https://clojars.org/org.replikativ/hasch"> <img src="https://img.shields.io/clojars/v/org.replikativ/hasch.svg" /></a>
<a href="https://circleci.com/gh/replikativ/hasch"><img src="https://circleci.com/gh/replikativ/hasch.svg?style=shield"/></a>
<a href="https://github.com/replikativ/hasch/tree"><img src="https://img.shields.io/github/last-commit/replikativ/hasch"/></a>
<a href="https://versions.deps.co/replikativ/hasch" title="Dependencies Status"><img src="https://versions.deps.co/replikativ/hasch/status.svg" /></a>
</p>

Cross-platform cryptographic hashing of [EDN](https://github.com/edn-format/edn) data structures for Clojure and ClojureScript.

## Overview

hasch produces deterministic, content-based hashes for EDN values. Commutative data structures (maps, sets, records) hash identically regardless of iteration order, preserving Clojure value semantics. Hashes are consistent across JVM and JavaScript runtimes.

Two hashing modules are provided with different strengths:

- **`hasch.core`** — string-based encoding with **transparent structural hashing** via `HashRef`. Replace any subtree with its `HashRef` and the parent hash stays identical — true merkle-style incremental hashing. Best for content-addressed storage, large nested structures, and cases where subtrees change independently.
- **`hasch.fast`** — binary-encoded streaming digest, **~6-14x faster** per-value throughput. Best for hashing many independent values (UUID generation, deduplication, signatures).

Both modules support all EDN types and [incognito](https://github.com/replikativ/incognito) record serialization. They produce different hash values and should not be mixed for the same data.

## Installation

Add to `deps.edn`:

```clojure
org.replikativ/hasch {:mvn/version "0.3.97"}
```

## Quick Start

### hasch.core — structural hashing

```clojure
(require '[hasch.core :as hc])

;; Generate UUID-5 from any EDN value
(hc/uuid {:name "Alice" :age 30})
;=> #uuid "..."

;; Transparent HashRef — same hash with or without:
(= (hc/uuid {:data [1 2 3]})
   (hc/uuid {:data (hc/hash-ref [1 2 3])}))  ;=> true

(hc/edn-hash [1 2 3])
;=> (120 75 53 ...)  ;; seq of unsigned bytes

(hc/b64-hash "hello")
;=> "..."
```

### hasch.fast — high throughput

```clojure
(require '[hasch.fast :as hf])

;; Same API, ~6-14x faster, different hash values
(hf/uuid {:name "Alice" :age 30})
;=> #uuid "2c6ea7f3-..."

;; Random UUID-4
(hf/uuid)
;=> #uuid "a27dfbb9-..."

;; Raw hash bytes
(hf/edn-hash [1 2 3])
;=> #object["[B" ...]

;; Hex string / Base64
(hf/hash->str (hf/edn-hash "hello"))
(hf/b64-hash "hello")

;; SHA-256 variant (faster, still cryptographically secure)
(hf/sha256-uuid {:data "value"})

;; Sequential UUIDs (time-prefixed, sortable)
(hf/squuid)
```

## Choosing a module

| | hasch.core | hasch.fast |
|---|---|---|
| **Encoding** | String (pr-str of numbers) | Binary (direct byte layout) |
| **Digest** | Intermediate byte arrays per value | Streaming (update MessageDigest in-place) |
| **Per-value speed** | Baseline | ~6-14x faster |
| **Structural hashing** | Transparent `HashRef` | — |
| **Best for** | Content-addressed stores, merkle trees, incremental rehashing | UUID generation, deduplication, high-throughput hashing |

For large nested structures where subtrees change independently, `hasch.core` with `HashRef` can be faster end-to-end than `hasch.fast`, because unchanged subtrees are never re-traversed.

## Structural Hashing with HashRef

`hasch.core` supports **transparent merkle-style structural hashing** via `HashRef`. A `HashRef` caches the coercion output of a value so that when it appears inside a larger structure, it produces the **same hash** as the original value — no re-traversal needed.

```clojure
(require '[hasch.core :as hc])

;; These produce identical hashes:
(hc/uuid {:data [1 2 3]})
(hc/uuid {:data (hc/hash-ref [1 2 3])})  ;; same! transparent substitution

;; Even edn-hash of a HashRef equals the original:
(= (hc/edn-hash x) (hc/edn-hash (hc/hash-ref x)))  ;; true for all x
```

This is useful for content-addressed storage where you want to:
- Hash large trees incrementally (only rehash the changed path)
- Avoid pulling entire subtrees into memory just to compute a parent hash
- Build git-like persistent data structures

```clojure
;; Hash subtrees once, reuse in parent structures
(def ref-a (hc/hash-ref {:name "Alice" :scores [1 2 3]}))
(def ref-b (hc/hash-ref {:name "Bob" :scores [4 5]}))

(hc/uuid {:users [ref-a ref-b]})

;; When sub-a changes, only rehash the changed subtree:
(def ref-a' (hc/hash-ref {:name "Alice" :scores [1 2 3 4]}))
(hc/uuid {:users [ref-a' ref-b]})  ;; ref-b reused as-is
```

Note: `HashRef` is only available in `hasch.core`. The streaming architecture of `hasch.fast` does not support transparent substitution.

## Record Hashing with incognito

Records are hashed via [incognito](https://github.com/replikativ/incognito), which normalizes JVM class names to a platform-neutral format (`my.ns.MyRecord` → `my.ns/MyRecord`). This ensures records hash identically across JVM and ClojureScript.

```clojure
(require '[hasch.fast :as hf])

;; Records with default serialization work automatically
(defrecord Person [name age])
(hf/uuid (->Person "Alice" 30))

;; Custom write-handlers for non-default serialization
(hf/edn-hash (->Person "Alice" 30)
             hf/sha512-message-digest
             {'my.ns.Person (fn [r] [(:name r)])})
```

## Why Not Clojure's `hash`?

Clojure's built-in `hash` is optimized for speed in data structures (murmur3, 32-bit) and intentionally trades collision resistance for performance. This is fine for hash maps but unacceptable for content-addressed systems where collisions could corrupt data. hasch uses SHA-512, providing cryptographic collision resistance suitable for distributed systems and untrusted environments.

## Extending to Custom Types

Extend `hasch.fast/PStreamHash` (or `hasch.benc/PHashCoercion` for hasch.core) to add hashing for your own types. However, in most cases it is simpler to make your type serializable via [incognito](https://github.com/replikativ/incognito).

Your implementation must satisfy the equality relation:
- `(= a b)` ⟹ `(= (edn-hash a) (edn-hash b))`
- `(not= a b)` ⟹ `(not= (edn-hash a) (edn-hash b))`

## Building

```bash
# Run JVM tests
clj -M:test

# Run CLJS tests (requires Node.js)
npx shadow-cljs compile node-test

# Build JAR
clj -T:build jar

# Install locally
clj -T:build install

# Deploy to Clojars
clj -T:build deploy
```

## Changes

- 0.3.97 Add `hasch.fast` module with binary streaming digest (~6-14x speedup). Add `HashRef` for merkle-style structural hashing. Add incognito support to `hasch.fast`.
- 0.3.5 Support BigInteger and BigDecimal hashing (same as for limited precision types).
- 0.3.4 Expose high-level base64 hashes with full precision.
- 0.3.2 Minimize dependencies, explicit profiles for different Clojure(Script) versions
- 0.3.1 fix bug in hashing sequences containing null
- 0.3.0 fix accidental hashing of records as maps
- 0.3.0-beta4 fix record serialization with incognito
- 0.3.0 Overhaul encoding for ~10x-20x times performance on the JVM. Use safe SHA-512. Add byte-array support for blobs.

## Contributors

- Max Penet
- James Conroy-Finn
- Konrad Kühne
- Christian Weilbach

## License

Copyright © 2014-2026 Christian Weilbach and contributors

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
