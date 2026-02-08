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

Two hashing modules are provided:

- **`hasch.core`** — the original string-based encoding. Stable hashes, backward compatible.
- **`hasch.fast`** — binary-encoded streaming digest. ~6-14x faster, produces different hashes.

Both modules support all EDN types, [incognito](https://github.com/replikativ/incognito) record serialization, and structural (merkle-style) hashing via `HashRef`.

## Installation

Add to `deps.edn`:

```clojure
org.replikativ/hasch {:mvn/version "0.3.97"}
```

## Quick Start

### hasch.fast (recommended for new projects)

```clojure
(require '[hasch.fast :as hf])

;; Generate UUID-5 from any EDN value
(hf/uuid {:name "Alice" :age 30})
;=> #uuid "2c6ea7f3-..."

;; Random UUID-4
(hf/uuid)
;=> #uuid "a27dfbb9-..."

;; Raw hash bytes
(hf/edn-hash [1 2 3])
;=> #object["[B" ...]

;; Hex string
(hf/hash->str (hf/edn-hash "hello"))
;=> "01046861..."

;; Base64
(hf/b64-hash "hello")
;=> "AQQA..."

;; SHA-256 variant (faster, still cryptographically secure)
(hf/sha256-uuid {:data "value"})
;=> #uuid "..."

;; Sequential UUIDs (time-prefixed, sortable)
(hf/squuid)
;=> #uuid "67a5c81e-..."
```

### hasch.core (legacy / backward compatible)

```clojure
(require '[hasch.core :as hc])

;; Same API, different (legacy) hash values
(hc/uuid {:name "Alice" :age 30})
;=> #uuid "..."

(hc/edn-hash [1 2 3])
;=> (120 75 53 ...)  ;; returns seq of unsigned bytes

(hc/b64-hash "hello")
;=> "..."
```

## hasch.fast vs hasch.core

| | hasch.fast | hasch.core |
|---|---|---|
| **Encoding** | Binary (direct byte layout) | String (pr-str of numbers) |
| **Digest** | Streaming (update MessageDigest in-place) | Intermediate byte arrays per value |
| **Speed** | ~6-14x faster | Baseline |
| **Hash format** | SHA-512 with version prefix | SHA-512 |
| **Compatibility** | New hashes | Stable, backward compatible |
| **Record support** | incognito | incognito |
| **Structural hashing** | `hasch.fast/HashRef` | `hasch.benc/HashRef` |

**When to use which:**
- **New projects**: Use `hasch.fast`. The binary encoding is more efficient and the streaming digest avoids intermediate allocations.
- **Existing data**: Use `hasch.core` if you have stored hashes that must remain stable (e.g., content-addressed storage, distributed logs).
- **Migration**: The two modules produce incompatible hashes. Do not mix them for the same data.

## Structural Hashing with HashRef

Both modules support **merkle-style structural hashing** via `HashRef`. A `HashRef` wraps pre-computed hash bytes so that when it appears inside a larger structure, the hash bytes are inlined directly instead of re-traversing the subtree.

This is useful for content-addressed storage where you want to:
- Hash large trees incrementally (only rehash the changed path)
- Avoid pulling entire subtrees into memory just to compute a parent hash
- Build git-like persistent data structures

```clojure
(require '[hasch.fast :as hf])

;; Hash a subtree once, wrap in HashRef
(def child-ref (hf/hash-ref {:large "subtree" :with [1 2 3]}))

;; Use the HashRef in a parent structure - the child's hash bytes
;; are inlined directly, no re-traversal needed
(hf/uuid {:metadata "parent" :child child-ref})

;; Changing the parent doesn't require re-hashing the child
(hf/uuid {:metadata "updated" :child child-ref})
```

**Important:** The two modules use different `HashRef` types that are not interchangeable:

- `hasch.fast/HashRef` implements `PStreamHash` — use only with `hasch.fast` functions
- `hasch.benc/HashRef` implements `PHashCoercion` — use only with `hasch.core` functions

Using the wrong type throws an error to prevent accidental hash corruption.

```clojure
;; hasch.core HashRef
(require '[hasch.core :as hc])
(def core-ref (hc/hash-ref {:data "value"}))
(hc/uuid {:parent true :child core-ref})  ;; OK
;; (hf/uuid {:parent true :child core-ref})  ;; ERROR!

;; hasch.fast HashRef
(def fast-ref (hf/hash-ref {:data "value"}))
(hf/uuid {:parent true :child fast-ref})  ;; OK
;; (hc/uuid {:parent true :child fast-ref})  ;; ERROR!
```

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
