(ns hasch.test
  (:require [hasch.core :refer [edn-hash uuid]]
            [hasch.platform :refer [uuid5 sha512-message-digest hash->str as-value
                                    padded-coerce]]
            #+clj  [clojure.test :as t
                    :refer (is deftest with-test run-tests testing)]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)]))


(defrecord Bar [name])

(deftest value-generalisation
  (testing "Testing correct (de)serialisation of records (tagged literals)."
    (is (= (as-value {:foo :bar
                      :hasch.test/one 'sym
                      :obj (Bar. "hello")})
           {:foo :bar,
            :hasch.test/one 'sym,
            :obj ['hasch.test.Bar
                  {:name "hello"}]}))))

(defn ehash [v]
  (map byte (edn-hash v)))


(deftest hash-test
  (testing "Basic hash coercions of EDN primitives."
    (is (= (ehash nil)
           '(-72 36 77 2 -119 -127 -42 -109 -81 123 69 106 -8 -17 -92 -54 -42 61 40 46 25 -1 20 -108 44 36 110 80 -39 53 29 34 112 74 -128 42 113 -61 88 11 99 112 -34 76 -21 41 60 50 74 -124 35 52 37 87 -44 -27 -61 -124 56 -16 -29 105 16 -18)))

    (is (= (ehash true)
           '(-35 -33 -4 44 103 48 51 -57 71 -72 -100 -69 -55 -116 35 99 -21 -103 -71 70 -99 -27 122 4 111 90 12 -106 43 67 -71 -90 -46 79 54 62 117 -83 76 -4 -69 67 -93 85 -54 124 63 -4 109 44 47 70 74 -127 52 -15 35 15 116 -3 -15 -115 50 -125)))

    (is (= (ehash false)
           '(54 0 110 63 -98 -119 -80 89 -36 -21 107 -43 84 -97 27 25 -108 -50 -63 96 -64 73 41 -1 -36 -75 -41 106 -48 -36 -83 69 -43 -66 -75 70 -115 -63 1 -31 -68 -114 127 -80 102 61 13 54 -105 -95 -61 -98 -104 -66 -44 -88 91 43 -103 108 122 123 90 32)))

    (is (= (ehash \f)
           '(-45 -123 -53 -32 -62 -82 -120 44 -40 77 98 85 54 -68 116 101 -117 -82 40 108 48 -76 -21 -25 -42 -67 34 -10 32 30 56 45 -77 -38 36 -50 61 -65 79 -96 -44 -94 -44 -30 -21 17 27 -28 -38 74 17 -27 9 -109 -69 -24 35 -12 -77 -23 66 -91 -104 -3)))

    (is (= (ehash \ä)
           '(51 -24 113 -18 -13 104 -40 10 -113 88 -113 111 122 -36 35 -118 -5 22 8 -126 -18 73 -3 62 -113 -49 -48 45 116 21 120 18 -3 34 -96 30 -112 46 -74 7 -96 -2 -59 120 -57 -36 -116 -47 3 66 25 -42 -125 -111 17 -34 28 -99 22 103 -30 -2 -78 -70)))

    (is (= (ehash "hello")
           '(-78 114 9 -13 3 -106 0 -124 -20 -40 60 87 108 34 2 35 85 37 -53 -54 97 -80 9 55 25 -65 -113 -5 -5 47 49 -117 99 -65 77 63 -89 -98 61 -73 -23 59 43 57 16 -4 121 -58 65 -55 112 -89 96 61 -122 122 -79 -107 45 87 -23 23 -83 -64)))

    (is (= (ehash "小鳩ちゃんかわいいなぁ")
           '(2 -65 84 39 34 44 -29 102 -121 109 17 -120 -97 80 -3 7 40 0 -86 -122 -58 -52 -119 10 -62 21 113 -53 2 87 125 80 -84 -91 111 110 -34 7 123 -118 -108 124 -49 -76 -16 -49 91 6 -8 28 53 -88 -113 30 106 103 101 82 -123 -41 69 35 93 47)))

    (is (= (ehash 1234567890)
           '(65 -57 -98 -92 -63 95 -43 -112 -23 29 41 86 123 106 110 -41 117 -31 -107 -7 -52 124 -36 -39 -30 120 -125 -78 61 -123 39 -28 -74 -23 -21 -7 10 -7 -115 122 101 25 46 -122 18 -34 -81 -32 -122 61 -89 114 15 109 2 -110 38 65 1 55 -128 -119 -112 55)))

    (is (= (ehash (double 123.1))
           (ehash (float 123.1))
           '(-101 -75 33 -4 126 113 -68 20 -46 -101 50 24 125 -44 -51 -96 -121 108 90 43 -102 65 61 -27 -30 83 11 110 64 61 124 45 43 -70 -104 127 64 -85 -85 -102 28 -107 -76 -120 -27 69 -61 -111 126 99 56 14 48 -62 -76 126 -44 83 123 -50 36 -67 -67 -89)))

    (is (= (ehash :core/test)
           '(62 51 -42 78 41 84 37 -51 69 -59 105 26 -21 55 30 87 46 117 -69 -62 101 -72 -117 -12 111 -24 98 -81 16 -82 -74 -45 11 -85 -102 64 90 18 -27 93 -68 -10 33 -22 102 -111 68 30 92 0 81 -48 -46 10 124 -119 -53 18 -7 -118 -30 -3 60 62)))

    (is (= (ehash  #uuid "242525f1-8ed7-5979-9232-6992dd1e11e4")
           '(42 -13 -73 -19 -23 94 -10 1 110 56 -25 49 64 -39 -75 17 108 11 120 -57 -33 53 -107 47 49 8 109 94 127 93 -6 51 -89 -45 25 31 3 -85 -107 67 23 -11 38 -8 40 31 -57 -45 -94 -14 120 99 -69 6 29 -19 53 -82 22 -64 27 -97 -29 -92)))

    (is (= (ehash (#+clj java.util.Date. #+cljs js/Date. 1000000000000))
           '(-79 -30 -44 -21 -35 67 -80 34 -72 69 101 45 117 -63 95 -69 54 50 -46 -107 10 -63 10 67 -36 -82 25 99 -80 115 -6 -40 29 49 -108 -89 52 86 -53 90 30 -86 62 -107 115 102 109 120 -128 62 2 -43 -68 41 -53 91 -54 106 -114 100 119 -96 26 3)))

    (is (= (ehash 'core/+)
           '(-92 63 64 77 -66 -112 72 80 34 36 -2 -19 101 99 57 114 54 44 -61 22 -1 11 -14 114 99 87 99 -121 103 73 -92 -73 20 -64 -72 54 -73 -12 -64 -105 88 96 55 -52 73 -100 73 92 -102 8 -8 -51 119 -99 34 112 -54 51 52 -87 -94 61 91 -21)))

    (is (= (ehash '(1 2 3))
           '(-12 105 -70 110 -73 117 -61 78 70 57 -5 -124 -123 114 -122 -81 -28 94 -14 41 -62 -65 -70 -19 -93 -78 -1 -63 -115 120 5 -119 -33 -126 -86 47 -25 -123 78 -125 -128 -62 115 -116 -70 -87 124 71 -51 -46 -28 -20 82 97 -90 -98 -66 98 106 80 -19 -107 96 102)))

    (is (= (ehash [1 2 3 4])
           '(-84 52 37 123 -77 106 -13 -49 88 -79 -38 22 -86 25 13 -101 -51 89 -100 -5 -3 50 3 3 -65 74 -27 97 -4 37 -94 -16 -59 -4 -16 -57 -79 8 96 -29 121 100 106 -124 68 -29 -81 -67 -9 -72 108 25 117 -102 -70 63 108 4 -46 20 75 25 -17 -57)))

    (is (= (ehash {:a "hello"
                   :balloon "world"})
           '(-95 -12 92 79 119 116 90 96 47 -70 -5 110 0 102 46 7 -41 -57 -101 -80 -39 14 80 -35 -65 -43 -41 17 -123 -50 58 -17 -50 -32 7 4 60 65 126 -19 -78 101 -4 90 0 -109 91 -1 -78 112 55 -60 -48 77 98 7 -29 -76 -114 -127 89 58 30 -47)))

    (is (= (ehash #{1 2 3 4})
           '(41 16 34 5 -16 -20 -30 -6 -70 -47 37 117 -42 -20 -67 115 -70 -104 41 -56 -117 124 127 107 111 -93 123 95 -16 -69 97 121 -73 101 117 -60 -117 115 60 67 -34 -18 104 125 106 89 11 -49 -42 -96 -45 39 7 92 -76 32 58 34 -91 -33 40 -113 33 -128)))

    (is (= (ehash (Bar. "hello"))
           '(-13 -40 -93 -97 -9 -120 113 106 65 -102 -27 -1 -33 -71 33 120 -99 -4 91 127 -77 -11 -6 34 -12 11 -107 125 -50 -106 19 49 3 -40 -45 -40 87 -111 63 -5 20 40 22 80 -75 -72 44 117 -7 -72 -87 110 -4 -83 36 -19 -119 -109 115 31 51 14 -78 -46)))))

(deftest padded-coercion
  (testing "Padded xor coercion for commutative collections."
    (is (= (map byte
                (padded-coerce (map byte-array
                                    [[0xa0 0x01] [0x0c 0xf0 0x5f] [0x0a 0x30 0x07]])
                               (sha512-message-digest)
                               sha512-message-digest))
           (map byte (padded-coerce (map byte-array
                                         [[0xa0 0x01] [0x0a 0x30 0x07] [0x0c 0xf0 0x5f]])
                                    (sha512-message-digest)
                                    sha512-message-digest))))))


(deftest code-hashing
  (testing "Code hashing."
    (is (= (-> '(fn fib [n]
                  (if (or (= n 0) (= n 1)) 1
                      (+ (fib (- n 1)) (fib (- n 2)))))
               edn-hash
               uuid5)
           #uuid "17d15a32-4ec3-5f13-80dc-2b91aa881ac3"))))

(deftest hash-stringification
  (testing "Stringification."
    (is (= (hash->str (range 256))
           "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"))))



#_(run-tests)


;; fire up repl
#_(do
    (ns dev)
    (def repl-env (reset! cemerick.austin.repls/browser-repl-env
                         (cemerick.austin/repl-env)))
    (cemerick.austin.repls/cljs-repl repl-env))



;; #<IllegalStateException java.lang.IllegalStateException: CRITICAL: Fetched trans ID: 197bf9d9-1edf-5a11-b4d9-e3ce09d58556 does not match HASH 23c147d1-35d5-5ae3-bfcf-50ac151f6bba for value #datascript/DB {:schema {:down-votes {:db/cardinality :db.cardinality/many}, :arguments {:db/cardinality :db.cardinality/many}, :up-votes {:db/cardinality :db.cardinality/many}, :hashtags {:db/cardinality :db.cardinality/many}, :posts {:db/cardinality :db.cardinality/many}}, :datoms []} from CLIENT-PEER>
