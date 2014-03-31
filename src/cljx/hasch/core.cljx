(ns ^:shared hasch.core
  "Hashing functions for EDN."
  (:require [hasch.benc :refer [IHashCoercion -coerce benc magics padded-coerce]]
            [hasch.platform :refer [sha-1 boolean? uuid? date? byte->hex]]
            #+cljs [cljs.reader :refer [read-string]] ;; remove after testing
            ))

(defn atom? [val]
  (or (nil? val)
      (boolean? val)
      (char? val)
      (symbol? val)
      (keyword? val)
      (string? val)
      (number? val)
      (uuid? val)
      (date? val)))

(defn edn-hash
  ([val] (edn-hash val sha-1))
  ([val hash-fn]
     (let [coercion (map byte (-coerce val hash-fn))]
       (if (atom? val)
         (hash-fn coercion)
         coercion))))


(defn hash->str [bytes]
  (apply str (map byte->hex bytes)))




#_(do
    (ns dev)
    (def repl-env (reset! cemerick.austin.repls/browser-repl-env
                         (cemerick.austin/repl-env)))
    (cemerick.austin.repls/cljs-repl repl-env))



#_(deftest hash-test
  (testing "Basic hash coercions of EDN primitives."
    (is (= (edn-hash nil)
           '(-68 -123 -55 -6 27 23 -13 -72 -30 78 -84 52 50 -1 -10 38 -9 86 101 -16)))

    (is (= (edn-hash true)
           '(-82 -53 -80 -97 -3 96 75 -36 61 87 14 -23 117 94 -33 -37 80 -122 40 -16)))

    (is (= (edn-hash false)
           '(66 70 -21 -52 104 -40 -56 -115 1 105 -50 -78 68 66 -52 -44 109 43 80 -115)))

    (is (= (edn-hash \f)
           '(43 12 -14 40 -112 54 62 110 -96 -1 -109 56 41 -123 5 -85 117 95 -105 -51)))

    (is (= (edn-hash \ä)
           '(-88 -103 -60 67 67 -33 -85 -63 -95 -49 -66 73 -75 95 51 114 -32 78 61 -40)))

    (is (= (edn-hash "hello")
           '(-8 107 -8 -32 97 -4 24 38 -15 87 52 43 97 -10 104 -98 -52 -62 -53 -18)))

    (is (= (edn-hash 1234567890)
           '(66 -122 -53 -14 127 -30 88 -126 54 -32 -76 105 -17 53 0 -95 -67 -97 -51 113)))

    (is (= (edn-hash (double 123.1))
           (edn-hash (float 123.1))
           '(-29 71 118 95 -34 -121 2 74 115 122 115 99 9 -29 81 -4 4 -1 40 26)))

    (is (= (edn-hash ::test)
           '(-89 89 56 -29 -66 48 77 68 -38 59 -121 -115 7 119 3 -108 38 -69 46 -22)))

    (is (= (edn-hash (read-string "#uuid \"242525f1-8ed7-5979-9232-6992dd1e11e4\""))
           '(107 12 -44 64 -80 -20 -112 59 113 41 9 1 -18 -8 100 -128 -119 72 -108 -74)))

    (is (= (edn-hash (java.util.Date. 1000000000000))
           '(-85 23 -41 81 81 15 84 -6 90 55 52 32 -4 -2 -2 -97 -121 85 -14 -49)))

    (is (= (edn-hash `+)
           (edn-hash 'clojure.core/+)
           '(-17 59 -74 21 -9 86 -117 -22 116 -117 109 -41 105 -49 121 -99 -1 40 -111 -47)))

    (is (= (edn-hash '+)
           '(-22 -42 87 43 -25 46 110 -37 -81 47 113 -56 -83 2 76 26 92 -79 -128 -68)))

    (is (= (edn-hash '(1 2 3))
           '(-72 38 65 -93 -79 110 -52 -71 -112 -55 111 -17 -103 60 114 -86 21 91 -40 95)))

    (is (= (edn-hash [1 2 3 4])
           '(-28 -45 -97 -97 -102 96 -39 -116 -11 72 86 100 -68 123 64 -45 19 -87 75 -123)))

    (is (= (edn-hash {:a "hello"
                      :balloon "world"})
           '(93 101 -35 100 -124 -7 43 -56 -66 103 -90 108 -5 -121 121 38 59 -37 -99 -62)))

    (is (= (edn-hash #{1 2 3 4})
           '(-102 -36 77 41 -3 -81 30 107 -53 26 -102 26 -66 59 32 -27 -49 -80 -61 45)))))


;; works basically...
#_(-coerce 1234567890 sha-1)
#_(-coerce 123.1 sha-1)

#_(-coerce true sha-1)
#_(-coerce false sha-1)

#_(-coerce nil sha-1)

#_(-coerce (js/Date. 1000000000000) sha-1)
#_(-coerce (read-string "#uuid \"242525f1-8ed7-5979-9232-6992dd1e11e4\"") sha-1)

#_(-coerce "hello" sha-1)
#_(-coerce '() sha-1)

;; contains chars instead of bytes
#_(-coerce ::test sha-1)
#_(-coerce '+)

;; different to jvm hash
#_(-coerce [1 2 3] sha-1)
#_(-coerce '(1 2 3) sha-1)

;; throws npe
#_(-coerce {} sha-1)
