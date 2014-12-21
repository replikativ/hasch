(defproject net.polyc0l0r/hasch "0.3.0-SNAPSHOT"
  :description "Cryptographic hashing of EDN datastructures."
  :url "http://github.com/ghubber/hasch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]]
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["target/classes"]
  :plugins [[lein-cljsbuild "1.0.3"]
            [com.keminglabs/cljx "0.4.0"]
            [com.cemerick/austin "0.1.4"]]

  :profiles {:dev {:dependencies [[com.cemerick/clojurescript.test "0.3.1"]
                                  [criterium "0.4.3"]]}}

  :hooks [cljx.hooks]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}

                  {:source-paths ["test/cljx"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["test/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}]}

  :jvm-opts ^:replace []

  :cljsbuild
  {:builds
   [{:source-paths ["src/cljs"
                    "target/classes"]
     :compiler
     {:output-to "target/cljs/testable.js"
      :optimizations :whitespace
      :pretty-print true}}]
   :test-commands {"unit-tests" ["phantomjs" "target/cljs/testable.js"]}})
