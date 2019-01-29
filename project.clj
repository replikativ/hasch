(defproject io.replikativ/hasch "0.3.5"
  :description "Cryptographic hashing of EDN datastructures."
  :url "http://github.com/replikativ/hasch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [io.replikativ/incognito "0.2.2"]
                 [org.clojure/data.codec "0.1.1"]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.1.4"]]

  :global-vars {*warn-on-reflection* true}
  :aliases {"all" ["with-profile" "default:+1.7:+1.8"]}
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.8"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [criterium "0.4.4"]]
                   :figwheel {:nrepl-port 7888
                              :nrepl-middleware ["cider.nrepl/cider-middleware"
                                                 "cemerick.piggieback/wrap-cljs-repl"]}
                   :plugins [[lein-figwheel "0.5.8"]]
                   :repl-options {; for nREPL dev you really need to limit output
                                  :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]
                                  [org.clojure/clojurescript "1.7.228"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.8.51"]]}}

  :jvm-opts ^:replace []

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
    #_{:id "main"
       :source-paths ["src"]
       :compiler
       {:output-to "resources/public/js/main.js"
        :optimizations :advanced}}
    {:id "test"
     :source-paths ["src" "test"]
     :compiler
     {:output-to "resources/private/js/test.js"
      :output-dir "resources/private/js/out"
      :optimizations :whitespace
    :pretty-print true}}]
  :test-commands {"unit-tests" ["phantomjs" "resources/private/js/test.js"
                                 "resources/private/html/unit-test.html"]}})
