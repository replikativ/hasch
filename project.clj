(defproject io.replikativ/hasch "0.3.0"
  :description "Cryptographic hashing of EDN datastructures."
  :url "http://github.com/replikativ/hasch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [io.replikativ/incognito "0.2.0"]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.1.2"]]

  :aliases {"all" ["with-profile" "default:+1.7:+1.8"]}
  :profiles {:dev {:dependencies [[criterium "0.4.4"]]
                   :plugins [[com.cemerick/austin "0.1.6"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/clojurescript "1.7.228"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.8.51"]]}}

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
