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



(deftest hash-test
  (testing "Basic hash coercions of EDN primitives."
    (is (= (edn-hash nil)
           '(-72 36 77 2 -119 -127 -42 -109 -81 123 69 106 -8 -17 -92 -54 -42 61 40 46 25 -1 20 -108 44 36 110 80 -39 53 29 34 112 74 -128 42 113 -61 88 11 99 112 -34 76 -21 41 60 50 74 -124 35 52 37 87 -44 -27 -61 -124 56 -16 -29 105 16 -18)))

    (is (= (edn-hash true)
           '(-35 -33 -4 44 103 48 51 -57 71 -72 -100 -69 -55 -116 35 99 -21 -103 -71 70 -99 -27 122 4 111 90 12 -106 43 67 -71 -90 -46 79 54 62 117 -83 76 -4 -69 67 -93 85 -54 124 63 -4 109 44 47 70 74 -127 52 -15 35 15 116 -3 -15 -115 50 -125)))

    (is (= (edn-hash false)
           '(54 0 110 63 -98 -119 -80 89 -36 -21 107 -43 84 -97 27 25 -108 -50 -63 96 -64 73 41 -1 -36 -75 -41 106 -48 -36 -83 69 -43 -66 -75 70 -115 -63 1 -31 -68 -114 127 -80 102 61 13 54 -105 -95 -61 -98 -104 -66 -44 -88 91 43 -103 108 122 123 90 32)))

    (is (= (edn-hash \f)
           '(86 126 98 -78 -96 -70 106 20 0 77 44 70 -98 -93 76 -31 -10 -98 -120 29 125 33 39 63 9 38 52 -97 55 -73 22 -109 -126 103 -76 110 116 -16 -112 79 -16 127 -93 106 20 72 31 -125 30 -99 98 -33 -103 27 38 -118 64 -58 -95 76 52 33 82 -94)))

    (is (= (edn-hash \ä)
           '(-114 60 63 109 -122 -30 43 -66 -114 -70 -55 10 -51 43 -44 -95 85 101 -122 46 89 22 -45 -56 5 37 36 -117 -5 50 31 40 -106 -31 27 104 71 -22 -40 -29 9 -7 -11 87 -95 -7 119 -98 107 1 -69 -35 44 42 -19 84 41 -8 110 -128 -104 71 18 127)))

    (is (= (edn-hash "hello")
           '(9 23 112 41 72 -76 112 114 -76 -106 55 2 -128 94 -92 102 85 -85 -87 -16 126 98 -113 -70 72 -62 -119 99 120 -32 -118 99 46 49 -4 -38 30 126 13 -43 -65 -84 -102 -89 -44 70 28 -28 -55 57 39 19 75 42 -126 59 73 66 100 53 98 -90 127 -1)))

    (is (= (edn-hash "小鳩ちゃんかわいいなぁ")
           '(-115 41 -19 -6 38 -26 108 79 -34 -115 121 -14 28 21 -74 119 -5 -76 73 45 -48 -86 44 77 -63 -7 -102 -112 -3 -39 -122 122 -104 -65 118 -23 -42 1 114 92 -31 -108 -110 54 -120 -70 6 -92 -71 -9 56 -105 2 -5 -4 -3 104 -83 110 92 119 -5 -23 -75)))

    (is (= (edn-hash 1234567890)
           '(65 -57 -98 -92 -63 95 -43 -112 -23 29 41 86 123 106 110 -41 117 -31 -107 -7 -52 124 -36 -39 -30 120 -125 -78 61 -123 39 -28 -74 -23 -21 -7 10 -7 -115 122 101 25 46 -122 18 -34 -81 -32 -122 61 -89 114 15 109 2 -110 38 65 1 55 -128 -119 -112 55)))

    (is (= (edn-hash (double 123.1))
           (edn-hash (float 123.1))
           '(-101 -75 33 -4 126 113 -68 20 -46 -101 50 24 125 -44 -51 -96 -121 108 90 43 -102 65 61 -27 -30 83 11 110 64 61 124 45 43 -70 -104 127 64 -85 -85 -102 28 -107 -76 -120 -27 69 -61 -111 126 99 56 14 48 -62 -76 126 -44 83 123 -50 36 -67 -67 -89)))

    (is (= (edn-hash :core/test)
           '(-119 47 33 -8 -44 -86 -50 107 -85 105 32 38 -120 -90 -52 -48 -11 111 -13 -22 44 -111 -115 118 88 45 -48 -1 -14 -79 82 90 117 104 -45 89 58 37 -45 64 -78 -28 102 -58 51 78 -10 65 25 76 35 -58 -88 40 -36 -100 117 -88 114 98 61 21 -13 -14)))

    (is (= (edn-hash  #uuid "242525f1-8ed7-5979-9232-6992dd1e11e4")
           '(89 -72 123 39 -125 -96 2 27 113 105 58 2 82 44 -48 -113 11 4 -7 -50 42 64 -21 -14 -9 -121 88 70 -73 -89 -21 -92 -54 -84 6 -21 -111 120 116 -112 100 -78 -109 16 46 -35 9 -97 1 37 -111 38 -64 -57 45 -53 -55 -69 -23 53 107 -88 68 -2)))

    (is (= (edn-hash (#+clj java.util.Date. #+cljs js/Date. 1000000000000))
           '(-79 -30 -44 -21 -35 67 -80 34 -72 69 101 45 117 -63 95 -69 54 50 -46 -107 10 -63 10 67 -36 -82 25 99 -80 115 -6 -40 29 49 -108 -89 52 86 -53 90 30 -86 62 -107 115 102 109 120 -128 62 2 -43 -68 41 -53 91 -54 106 -114 100 119 -96 26 3)))

    (is (= (edn-hash 'core/+)
           '(-119 -66 80 -114 -103 -37 -70 117 73 -126 -64 -12 -38 -10 -113 0 -35 113 -23 -77 -61 85 -85 93 59 58 84 22 -38 9 -73 -9 -103 -5 19 -56 11 -116 -11 83 -29 -101 77 -96 -100 -36 112 58 -19 50 -103 34 -46 55 77 91 -1 44 -90 -100 -54 -16 -118 -62)))

    (is (= (edn-hash '(1 2 3))
           '(-115 37 -11 63 -57 62 -101 -62 -110 -21 86 -15 -50 -54 -1 -87 27 -31 123 -78 33 -90 18 30 -38 53 29 40 127 74 -96 119 84 58 -90 -77 -55 -19 -100 -92 125 64 -1 24 -122 37 12 -123 108 121 39 -124 92 -62 -34 91 -1 -115 122 16 37 -118 -30 74)))

    (is (= (edn-hash [1 2 3 4])
           '(-16 -58 -19 62 -100 -78 106 109 -90 30 112 109 -85 -104 -76 -105 -58 117 36 102 7 1 -22 -9 -18 -37 -97 117 110 -55 -77 108 48 -60 110 -96 -40 21 -31 -110 -37 -124 51 -63 127 52 91 -36 -47 127 -38 -105 3 -14 -39 -50 7 59 -128 108 -12 96 -33 17)))

    (is (= (edn-hash {:a "hello"
                      :balloon "world"})
           '(-50 83 -95 27 -128 -119 -126 -125 37 -12 -46 -26 16 72 -81 -20 84 -103 82 33 104 29 31 -61 61 5 118 -46 -2 -108 -36 -64 90 5 -88 -41 76 3 106 -19 51 108 121 23 -46 100 58 52 -106 105 -41 76 99 79 14 99 117 77 -97 70 -44 121 110 32)))

    (is (= (edn-hash #{1 2 3 4})
           '(97 15 35 18 29 98 22 85 -76 -35 45 45 64 71 -88 75 -38 -78 63 -112 -38 102 9 -121 46 38 21 -121 45 112 103 -101 22 -32 97 -95 123 95 70 -58 -53 -37 37 -116 123 22 114 -15 37 10 25 -44 -6 87 -83 -59 -37 -29 -36 -15 -20 -110 94 118)))

    (is (= (edn-hash (Bar. "hello"))
           '(102 80 -85 -61 106 -49 83 -18 57 -56 -81 110 -27 -16 78 124 -74 4 -98 46 60 71 -106 86 115 -28 77 -6 100 63 7 -38 30 -43 63 -17 -65 102 31 110 -126 106 26 73 48 35 -63 -14 7 -74 -16 -34 72 -34 -90 21 -8 -117 81 -56 28 -90 -34 9)))))

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
           #uuid "39187c70-9212-5eeb-b176-f04d97068ca1"))))

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
