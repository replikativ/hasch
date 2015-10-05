(defproject io.replikativ/hasch "0.3.0-SNAPSHOT"
  :description "Cryptographic hashing of EDN datastructures."
  :url "http://github.com/ghubber/hasch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [io.replikativ/incognito "0.1.2"]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.0.6"]]

  :profiles {:dev {:dependencies [[criterium "0.4.3"]]
                   :plugins [[com.cemerick/austin "0.1.6"]]}}

  :jvm-opts ^:replace []

  :cljsbuild
  {:builds
   {:main {:source-paths ["src"]
           :compiler
           {:output-to "resources/public/js/main.js"
            :optimizations :advanced}}
    :test {:source-paths ["src"]
           :compiler
           {:output-to "target/cljs/testable.js"
            :optimizations :whitespace
            :pretty-print true}}}
   :test-commands {"unit-tests" ["phantomjs" "target/cljs/testable.js"]}})
