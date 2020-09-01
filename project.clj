(defproject io.replikativ/hasch "0.3.8-SNAPSHOT"
  :description "Cryptographic hashing of EDN datastructures."
  :url "http://github.com/replikativ/hasch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.764" :scope "provided"]
                 [io.replikativ/incognito "0.2.5"]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.1.8"]]

  :global-vars {*warn-on-reflection* true}
  :aliases {"all" ["with-profile" "default:+1.8:+1.9"]}
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.19"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [criterium "0.4.6"]]
                   :figwheel {:nrepl-port 7888
                              :nrepl-middleware ["cider.nrepl/cider-middleware"
                                                 "cemerick.piggieback/wrap-cljs-repl"]}
                   :plugins [[lein-figwheel "0.5.8"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.8.51"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.9.293"]]}}

  :jvm-opts ^:replace []

  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :sign-releases false}]]

  :cljsbuild
  {:builds
   [{:id "cljs_repl"
     :source-paths ["src"]
     :figwheel true
     :compiler
     {:main hasch.core
      :asset-path "js/out"
      :output-to "resources/public/js/client.js"
      :output-dir "resources/public/js/out"
      :optimizations :none
      :pretty-print true}}
    {:id "test"
     :source-paths ["src" "test"]
     :compiler
     {:output-to "resources/test/compiled.js"
      :optimizations :whitespace
      :pretty-print true}}]
   :test-commands {"unit-tests" ["phantomjs" "resources/test/compiled.js"
                                 "resources/test/unit-test.html"]}})
