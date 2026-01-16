# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

hasch is a cross-platform library for consistently crypto-hashing EDN data structures on Clojure and ClojureScript using SHA-512. The key feature is that commutative data structures (maps, sets, records) hash to the same value independent of order, preserving Clojure value semantics.

Current artifact: `org.replikativ/hasch` on Clojars

## Project Structure

- `src/hasch/`: Core library code (all `.cljc` files for cross-platform compatibility)
  - `core.cljc`: Main API (`edn-hash`, `uuid`, `b64-hash`)
  - `benc.cljc`: Binary encoding protocol (`PHashCoercion`)
  - `platform.clj` / `platform.cljs`: Platform-specific implementations (SHA-512, UUID generation)
  - `base64.cljc`, `hex.cljc`, `md5.cljc`: Encoding utilities
- `test/hasch/`: Test files
  - `api_test.cljc`: Core API tests
  - `datahike_test.clj`: Integration tests with Datahike
- `build.clj`: Build automation using tools.build
- `deps.edn`: Dependency management
- `shadow-cljs.edn`: ClojureScript build configuration
- `tests.edn`: Kaocha test configuration

## Build Commands

**Clojure (JVM):**
```bash
# Run JVM tests
clj -M:test

# Run integration tests
clj -M:test --focus :integration

# Format check
clj -M:format

# Auto-fix formatting
clj -M:ffix

# Build JAR
clj -T:build jar

# Install locally
clj -T:build install

# Deploy to Clojars (requires CLOJARS_USERNAME and CLOJARS_PASSWORD env vars)
clj -T:build deploy
```

**ClojureScript:**
```bash
# Run ClojureScript tests (browser)
npx shadow-cljs watch browser-test

# Run ClojureScript tests (node)
npx shadow-cljs watch node-test

# Run CI tests (karma)
npm run ci-test

# Compile app build
npx shadow-cljs compile app
```

## Development

**Running a single test:**
```bash
# JVM - use kaocha's focus metadata or run specific namespace
clj -M:test --focus hasch.api-test

# ClojureScript - modify shadow-cljs.edn :node-test build to specify test namespace
```

**REPL development:**
```bash
# Clojure REPL
clj -M:dev

# ClojureScript REPL
npx shadow-cljs cljs-repl app
```

## CI/CD

The project uses CircleCI with custom orbs (`replikativ/clj-tools@0`) that handle:
- Setup (with ClojureScript support)
- Build (both Clojure and ClojureScript)
- Format checking
- Unit tests (JVM)
- ClojureScript tests
- Integration tests
- Auto-deployment to Clojars (on main branch)
- GitHub releases (on main branch)

## Architecture Notes

**Hash Coercion Protocol:**
The `PHashCoercion` protocol in `benc.cljc` is the core extension point. It defines `-coerce` which converts values to bytes for hashing. Platform-specific implementations in `platform.clj` and `platform.cljs` extend this for all EDN types.

**Commutative Hashing:**
Maps and sets use XOR-based hashing (`xor-hashes` in `benc.cljc`) to ensure order-independence. This takes max 32 bytes into account to avoid collisions while maintaining performance.

**Cross-Platform Compatibility:**
- Record tag names are normalized (JVM format `foo.bar_baz.Bar` → ClojureScript format `foo.bar-baz/Bar`) before hashing
- Uses incognito library for record serialization compatibility
- Platform-specific code isolated to `platform.clj` and `platform.cljs`

**Safety vs Speed:**
The library prioritizes cryptographic safety over speed. Large collections are hashed efficiently but sequentially. The split-size threshold (1024 bytes) in `benc.cljc` determines when to rehash vs direct encoding.

## Version Management

Version is auto-generated in `build.clj` as `0.3.<git-commit-count>`. The library uses semantic versioning with the patch number derived from git commit count.

## Testing with incognito

The library depends on `io.replikativ/incognito` for record serialization. Tests in `api_test.cljc` verify hash consistency with incognito's tagged literal approach.

## Extending to Custom Types

Extend `PHashCoercion` protocol to your types (see `IRecord` implementation in platform files). Your implementation must satisfy:
- Equality relation: `(= a b)` ⟺ `(= (edn-hash a) (edn-hash b))`
- Reflexivity, symmetry, transitivity
- Use `(:literal magics)` tag to avoid collisions
