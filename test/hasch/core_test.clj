(ns hasch.core-test
  (:require [clojure.test :refer :all]
            [hasch.core :refer :all]
            [hasch.platform :refer :all]
            [hasch.benc :refer :all]))


(deftest hash-test
  (testing "Basic hash coercions of EDN primitives."
    (is (= (edn-hash nil)
           '(-68 -123 -55 -6 27 23 -13 -72 -30 78 -84 52 50 -1 -10 38 -9 86 101 -16)))

    (is (= (edn-hash true)
           '(-82 -53 -80 -97 -3 96 75 -36 61 87 14 -23 117 94 -33 -37 80 -122 40 -16)))

    (is (= (edn-hash false)
           '(66 70 -21 -52 104 -40 -56 -115 1 105 -50 -78 68 66 -52 -44 109 43 80 -115)))

    (is (= (edn-hash \f)
           '(33 -67 -14 80 -102 66 -114 3 54 -50 -21 115 52 -123 2 16 -28 -38 119 89)))

    (is (= (edn-hash \ä) ;; does not work
           '(-100 75 110 80 124 -2 20 11 -34 116 21 3 83 -53 89 124 103 108 -77 -103)))

    (is (= (edn-hash "hello")
           '(-8 107 -8 -32 97 -4 24 38 -15 87 52 43 97 -10 104 -98 -52 -62 -53 -18)))

    (is (= (edn-hash 1234567890)
           '(66 -122 -53 -14 127 -30 88 -126 54 -32 -76 105 -17 53 0 -95 -67 -97 -51 113)))

    (is (= (edn-hash (double 123.1))
           (edn-hash (float 123.1))
           '(-29 71 118 95 -34 -121 2 74 115 122 115 99 9 -29 81 -4 4 -1 40 26)))

    (is (= (edn-hash :core/test)
           '(117 83 122 -68 -88 27 24 95 -77 -101 64 60 -93 96 74 -23 50 -91 -57 -48)))

    (is (= (edn-hash (read-string "#uuid \"242525f1-8ed7-5979-9232-6992dd1e11e4\""))
           '(107 12 -44 64 -80 -20 -112 59 113 41 9 1 -18 -8 100 -128 -119 72 -108 -74)))

    (is (= (edn-hash (java.util.Date. 1000000000000))
           '(-85 23 -41 81 81 15 84 -6 90 55 52 32 -4 -2 -2 -97 -121 85 -14 -49)))

    (is (= (edn-hash 'core/+)
           '(-71 -29 27 -64 -2 -5 120 17 -36 -12 57 -63 119 -50 -26 44 -2 -28 -116 -50)))

    (is (= (edn-hash '(1 2 3))
           '(-72 38 65 -93 -79 110 -52 -71 -112 -55 111 -17 -103 60 114 -86 21 91 -40 95)))

    (is (= (edn-hash [1 2 3 4])
           '(-28 -45 -97 -97 -102 96 -39 -116 -11 72 86 100 -68 123 64 -45 19 -87 75 -123)))

    (is (= (edn-hash {:a "hello"
                      :balloon "world"})
           '(93 101 -35 100 -124 -7 43 -56 -66 103 -90 108 -5 -121 121 38 59 -37 -99 -62)))

    (is (= (edn-hash #{1 2 3 4})
           '(-102 -36 77 41 -3 -81 30 107 -53 26 -102 26 -66 59 32 -27 -49 -80 -61 45)))))



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
           #uuid "265274e2-f93e-529c-8a95-30f54f7189b0"))))

(deftest hash-stringification
  (testing "Stringification."
    (is (= (hash->str (range 256))
           "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"))))


#_(run-tests)
