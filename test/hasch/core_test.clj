(ns hasch.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [hasch.core :refer [edn-hash uuid]]
            [hasch.platform :refer [uuid5 sha-1 hash->str]]
            [hasch.benc :refer [padded-coerce]]))


(uuid)

(deftest hash-test
  (testing "Basic hash coercions of EDN primitives."
    (is (= (edn-hash nil)
           '(-43 122 40 19 96 -80 57 126 23 -3 68 -111 83 -21 88 -92 125 -43 -79 44)))

    (is (= (edn-hash true)
           '(-13 103 -110 -22 -121 32 21 77 -9 69 52 6 -96 23 65 -17 -44 -51 14 68)))

    (is (= (edn-hash false)
           '(-102 -11 26 126 -99 111 -104 9 -119 -55 111 109 115 -88 -69 -92 -17 -120 117 77)))

    (is (= (edn-hash \f)
           '(34 -53 -90 -78 -87 125 96 30 64 101 -41 -128 -5 84 -43 -89 71 -90 -11 7)))

    (is (= (edn-hash \ä)
           '(40 34 -91 67 -20 37 -77 76 -120 -52 86 -112 61 60 -23 112 63 90 -35 -70)))

    (is (= (edn-hash "hello")
           '(-44 124 -21 53 -4 -124 -79 -30 -72 12 73 -74 106 0 73 -29 101 30 90 95)))

    (is (= (edn-hash "小鳩ちゃんかわいいなぁ")
           '(45 78 99 64 68 86 -13 -106 48 -42 -4 -128 -49 -75 44 122 84 -31 -33 122)))

    (is (= (edn-hash 1234567890)
           '(85 114 -52 44 12 -95 101 107 86 -20 67 13 -68 -96 51 -91 -82 -40 -119 -69)))

    (is (= (edn-hash (double 123.1))
           (edn-hash (float 123.1))
           '(-117 -40 -87 103 54 -30 103 96 -15 71 88 -86 64 30 -12 -108 -35 12 53 -126)))

    (is (= (edn-hash :core/test)
           '(66 -96 -109 23 -80 12 -115 -96 -98 80 38 -101 50 107 39 10 10 -29 98 106)))

    (is (= (edn-hash (read-string "#uuid \"242525f1-8ed7-5979-9232-6992dd1e11e4\""))
           '(-19 -9 47 46 126 -118 -46 -65 -71 -101 -26 -4 80 -120 -2 -29 60 16 -51 -69)))

    (is (= (edn-hash (java.util.Date. 1000000000000))
           '(-1 -47 18 -67 -42 3 58 -67 67 44 -79 -96 81 2 -48 -32 46 48 -8 -128)))

    (is (= (edn-hash 'core/+)
           '(75 -92 115 6 120 -61 61 -22 25 -60 66 110 107 -124 -15 -71 -115 -60 64 40)))

    (is (= (edn-hash '(1 2 3))
           '(-116 44 27 -100 84 -61 19 84 99 -104 46 -10 -76 73 15 117 -120 -114 29 -30)))

    (is (= (edn-hash [1 2 3 4])
           '(72 -82 26 57 -64 -48 67 -105 87 61 -82 83 93 105 24 15 -35 111 65 -105)))

    (is (= (edn-hash {:a "hello"
                      :balloon "world"})
           '(71 88 -9 109 68 7 -47 86 -3 84 -57 61 69 49 -58 26 -22 55 -22 -107)))

    (is (= (edn-hash #{1 2 3 4})
           '(103 23 -84 -86 51 89 46 111 17 -38 15 44 111 -121 120 -14 -72 35 -110 -114)))))



(deftest padded-coercion
  (testing "Padded xor coercion for commutative collections."
    (is (= (padded-coerce [[0xa0 0x01] [0x0c 0xf0 0x5f] [0x0a 0x30 0x07]] sha-1)
           (padded-coerce [[0xa0 0x01] [0x0a 0x30 0x07] [0x0c 0xf0 0x5f]] sha-1)))))


(deftest code-hashing
  (testing "Code hashing."
    (is (= (-> '(fn fib [n]
                  (if (or (= n 0) (= n 1)) 1
                      (+ (fib (- n 1)) (fib (- n 2)))))
               edn-hash
               sha-1
               uuid5)
           #uuid "0640c5d1-201c-537b-9407-8b5af8883ff3"))))

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
