(defproject hasch "0.1.0-SNAPSHOT"
  :description "Cryptographic hashing of EDN datastructures."
  :url "http://github.com/ghubber/hasch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2156"]]
  :source-paths ["src/clj" "src/cljs"]
  :plugins [[lein-cljsbuild "1.0.2"]
            [com.keminglabs/cljx "0.3.2"]
            [com.cemerick/austin "0.1.4"]]

  :hooks [cljx.hooks]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}]}


  :cljsbuild
  {:builds
   [{:source-paths ["src/cljs"
                    "target/classes"]
     :compiler
     {:output-to "resources/public/js/hasch.js"
      :optimizations :simple
      :pretty-print true}}]})
