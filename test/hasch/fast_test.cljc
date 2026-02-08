(ns hasch.fast-test
  (:require [hasch.fast :refer [edn-hash uuid uuid4 squuid b64-hash hash->str
                                sha256-uuid sha256-message-digest sha512-message-digest]
             :as hf]
            [hasch.core :as hc]
            [hasch.benc :as benc]
            [incognito.base :as ic]
            #?(:clj [clojure.test :as t :refer (is deftest run-tests testing)]
               :cljs [cljs.test :as t :refer-macros (is deftest run-tests testing)])))

(defrecord Bar [name])

;; =============================================================================
;; Helper to convert hash bytes to comparable unsigned seq
;; =============================================================================

(defn hash->unsigned-seq [hash-bytes]
  #?(:clj (map #(bit-and (int %) 0xff) hash-bytes)
     :cljs (vec hash-bytes)))

;; =============================================================================
;; Basic type hashing - determinism regression tests
;; =============================================================================

(deftest hash-nil-test
  (testing "nil hashes deterministically"
    (is (= (hash->unsigned-seq (edn-hash nil))
           (hash->unsigned-seq (edn-hash nil)))
        "Same input should produce same hash")))

(deftest hash-boolean-test
  (testing "Boolean hashing"
    (is (= (hash->unsigned-seq (edn-hash true))
           (hash->unsigned-seq (edn-hash true))))
    (is (= (hash->unsigned-seq (edn-hash false))
           (hash->unsigned-seq (edn-hash false))))
    (is (not= (hash->unsigned-seq (edn-hash true))
              (hash->unsigned-seq (edn-hash false)))
        "true and false should hash differently")))

(deftest hash-integer-test
  (testing "Integer hashing"
    (is (= (hash->unsigned-seq (edn-hash 42))
           (hash->unsigned-seq (edn-hash 42))))
    (is (not= (hash->unsigned-seq (edn-hash 42))
              (hash->unsigned-seq (edn-hash 43))))
    ;; Integer types should normalize
    (is (= (hash->unsigned-seq (edn-hash (int 1234567890)))
           (hash->unsigned-seq (edn-hash (long 1234567890))))
        "int and long should hash the same")))

(deftest hash-float-test
  (testing "Float/double hashing"
    (is (= (hash->unsigned-seq (edn-hash 3.14))
           (hash->unsigned-seq (edn-hash 3.14))))
    #?(:clj
       ;; JVM: float and double have different binary representations (precision loss)
       (is (not= (hash->unsigned-seq (edn-hash (double 123.1)))
                 (hash->unsigned-seq (edn-hash (float 123.1))))
           "float and double differ in binary encoding (precision loss)"))))

(deftest hash-string-test
  (testing "String hashing"
    (is (= (hash->unsigned-seq (edn-hash "hello"))
           (hash->unsigned-seq (edn-hash "hello"))))
    (is (not= (hash->unsigned-seq (edn-hash "hello"))
              (hash->unsigned-seq (edn-hash "world"))))))

(deftest hash-unicode-test
  (testing "Unicode string hashing"
    (is (= (hash->unsigned-seq (edn-hash "小鳩ちゃんかわいいなぁ"))
           (hash->unsigned-seq (edn-hash "小鳩ちゃんかわいいなぁ"))))
    (is (= (hash->unsigned-seq (edn-hash "😡😡😡"))
           (hash->unsigned-seq (edn-hash "😡😡😡"))))))

(deftest hash-character-test
  (testing "Character hashing"
    (is (= (hash->unsigned-seq (edn-hash \f))
           (hash->unsigned-seq (edn-hash \f))))
    (is (= (hash->unsigned-seq (edn-hash \ä))
           (hash->unsigned-seq (edn-hash \ä))))
    (is (not= (hash->unsigned-seq (edn-hash \f))
              (hash->unsigned-seq (edn-hash \g))))))

(deftest hash-keyword-test
  (testing "Keyword hashing"
    (is (= (hash->unsigned-seq (edn-hash :core/test))
           (hash->unsigned-seq (edn-hash :core/test))))
    (is (not= (hash->unsigned-seq (edn-hash :foo))
              (hash->unsigned-seq (edn-hash :bar))))
    (is (not= (hash->unsigned-seq (edn-hash :ns/foo))
              (hash->unsigned-seq (edn-hash :foo)))
        "Namespaced and non-namespaced keywords should differ")))

(deftest hash-symbol-test
  (testing "Symbol hashing"
    (is (= (hash->unsigned-seq (edn-hash 'core/+))
           (hash->unsigned-seq (edn-hash 'core/+))))
    (is (not= (hash->unsigned-seq (edn-hash 'foo))
              (hash->unsigned-seq (edn-hash 'bar))))))

(deftest hash-uuid-test
  (testing "UUID hashing"
    (is (= (hash->unsigned-seq (edn-hash #uuid "242525f1-8ed7-5979-9232-6992dd1e11e4"))
           (hash->unsigned-seq (edn-hash #uuid "242525f1-8ed7-5979-9232-6992dd1e11e4"))))))

(deftest hash-date-test
  (testing "Date hashing"
    (is (= (hash->unsigned-seq (edn-hash (#?(:clj java.util.Date. :cljs js/Date.) 1000000000000)))
           (hash->unsigned-seq (edn-hash (#?(:clj java.util.Date. :cljs js/Date.) 1000000000000)))))))

;; =============================================================================
;; Collection hashing
;; =============================================================================

(deftest hash-list-test
  (testing "List hashing"
    (is (= (hash->unsigned-seq (edn-hash '(1 2 3)))
           (hash->unsigned-seq (edn-hash '(1 2 3)))))
    (is (not= (hash->unsigned-seq (edn-hash '(1 2 3)))
              (hash->unsigned-seq (edn-hash '(3 2 1))))
        "Lists are order-dependent")))

(deftest hash-vector-test
  (testing "Vector hashing"
    (is (= (hash->unsigned-seq (edn-hash [1 2 3 4]))
           (hash->unsigned-seq (edn-hash [1 2 3 4]))))
    (is (not= (hash->unsigned-seq (edn-hash [1 2 3 4]))
              (hash->unsigned-seq (edn-hash [4 3 2 1])))
        "Vectors are order-dependent")))

(deftest hash-map-test
  (testing "Map hashing - order independent"
    (is (= (hash->unsigned-seq (edn-hash {:a "hello" :balloon "world"}))
           (hash->unsigned-seq (edn-hash {:a "hello" :balloon "world"}))))
    (is (= (hash->unsigned-seq (edn-hash {:a 1 :b 2 :c 3}))
           (hash->unsigned-seq (edn-hash {:c 3 :a 1 :b 2})))
        "Map hash should be order independent")))

(deftest hash-set-test
  (testing "Set hashing - order independent"
    (is (= (hash->unsigned-seq (edn-hash #{1 2 3 4}))
           (hash->unsigned-seq (edn-hash #{1 2 3 4}))))
    (is (= (hash->unsigned-seq (edn-hash #{1 2 3 4}))
           (hash->unsigned-seq (edn-hash #{4 3 2 1})))
        "Set hash should be order independent")))

(deftest hash-nested-test
  (testing "Nested data structure hashing"
    (is (= (hash->unsigned-seq (edn-hash {:a [1 2 3] :b #{:x :y} :c {:d "hello"}}))
           (hash->unsigned-seq (edn-hash {:a [1 2 3] :b #{:x :y} :c {:d "hello"}}))))))

(deftest hash-byte-array-test
  (testing "Byte array hashing"
    (is (= (hash->unsigned-seq
            (edn-hash #?(:cljs (js/Uint8Array. #js [1 2 3 42 149])
                         :clj (byte-array [1 2 3 42 149]))))
           (hash->unsigned-seq
            (edn-hash #?(:cljs (js/Uint8Array. #js [1 2 3 42 149])
                         :clj (byte-array [1 2 3 42 149]))))))))

;; =============================================================================
;; Type distinction tests
;; =============================================================================

(deftest distinct-types-test
  (testing "Different types produce different hashes"
    (let [h (fn [v] (hash->unsigned-seq (edn-hash v)))]
      ;; nil vs false vs 0 vs "" vs [] vs {}
      (is (not= (h nil) (h false)))
      (is (not= (h nil) (h 0)))
      (is (not= (h nil) (h "")))
      (is (not= (h false) (h 0)))
      #?(:clj
         (is (not= (h 0) (h 0.0))
             "Integer and float zero should hash differently (JVM only - CLJS has single number type)"))
      (is (not= (h []) (h '())))
      (is (not= (h []) (h #{})))
      (is (not= (h {}) (h #{})))
      (is (not= (h :foo) (h "foo")))
      (is (not= (h :foo) (h 'foo)))
      (is (not= (h "foo") (h 'foo))))))

;; =============================================================================
;; UUID generation tests
;; =============================================================================

(deftest uuid-determinism-test
  (testing "UUID is deterministic for same input"
    (is (= (uuid 42) (uuid 42)))
    (is (= (uuid "hello") (uuid "hello")))
    (is (= (uuid {:a 1 :b 2}) (uuid {:b 2 :a 1}))
        "UUID should be order-independent for maps")))

(deftest uuid-different-inputs-test
  (testing "UUID differs for different inputs"
    (is (not= (uuid 42) (uuid 43)))
    (is (not= (uuid "hello") (uuid "world")))))

(deftest uuid4-test
  (testing "uuid4 generates random UUIDs"
    (is (not= (uuid4) (uuid4))
        "Random UUIDs should differ")))

(deftest uuid-no-args-test
  (testing "uuid with no args generates random UUID"
    (is (not= (uuid) (uuid))
        "(uuid) should generate random UUIDs")))

(deftest sha256-uuid-test
  (testing "SHA-256 based UUID"
    (is (= (sha256-uuid 42) (sha256-uuid 42)))
    (is (not= (sha256-uuid 42) (uuid 42))
        "SHA-256 and SHA-512 UUIDs should differ")))

(deftest squuid-test
  (testing "Sequential UUID"
    (is (= (subs (str (squuid (uuid [1 2 3]))) 8)
           (subs (str (squuid (uuid [1 2 3]))) 8))
        "squuid with same base UUID should have same suffix")))

;; =============================================================================
;; Hash string and base64 tests
;; =============================================================================

(deftest hash->str-test
  (testing "Hash to hex string conversion"
    (is (string? (hash->str (edn-hash 42))))
    (is (= 128 (count (hash->str (edn-hash 42))))
        "SHA-512 should produce 128 hex chars")))

(deftest b64-hash-test
  (testing "Base64 hash encoding"
    (is (string? (b64-hash 42)))
    (is (= (b64-hash [1 2 3]) (b64-hash [1 2 3]))
        "b64-hash should be deterministic")))

;; =============================================================================
;; Code hashing
;; =============================================================================

(deftest code-hashing-test
  (testing "Code forms hash deterministically"
    (is (= (uuid '(fn fib [n]
                    (if (or (= n 0) (= n 1)) 1
                        (+ (fib (- n 1)) (fib (- n 2))))))
           (uuid '(fn fib [n]
                    (if (or (= n 0) (= n 1)) 1
                        (+ (fib (- n 1)) (fib (- n 2))))))))))

;; =============================================================================
;; SHA-256 message digest
;; =============================================================================

(deftest sha256-edn-hash-test
  (testing "SHA-256 edn-hash produces shorter hash"
    (let [h256 (edn-hash 42 sha256-message-digest)
          h512 (edn-hash 42 sha512-message-digest)]
      (is (= 32 #?(:clj (alength ^bytes h256) :cljs (alength h256)))
          "SHA-256 should produce 32 bytes")
      (is (= 64 #?(:clj (alength ^bytes h512) :cljs (alength h512)))
          "SHA-512 should produce 64 bytes"))))

;; =============================================================================
;; Collision resistance
;; =============================================================================

(deftest collision-resistance-test
  (testing "No collisions in 10K sequential items"
    (let [items (mapv (fn [i] {:file "test.cljs" :line i :column (mod i 10)}) (range 10000))
          hashes (into #{} (map #(hash->str (edn-hash %))) items)]
      (is (= 10000 (count hashes))
          "10K distinct items should produce 10K distinct hashes"))))

;; =============================================================================
;; Cross-platform regression values
;; These values are the expected hashes — must match on both JVM and CLJS.
;; If these fail, it means the encoding changed and hashes are no longer stable.
;; =============================================================================

(deftest regression-nil-hash
  (testing "nil hash regression"
    (is (= (hash->unsigned-seq (edn-hash nil))
           '(170 39 217 216 145 40 38 58 73 146 136 176 250 45 123 232 169 196 160 171 18 58 161 197 217 39 80 208 61 85 96 101 49 138 177 87 150 35 44 27 216 60 137 181 2 75 229 168 192 111 191 158 228 204 150 155 1 104 171 218 173 116 159 93)))))

(deftest regression-true-hash
  (testing "true hash regression"
    (is (= (hash->unsigned-seq (edn-hash true))
           '(116 101 146 24 138 241 24 97 93 198 17 163 184 72 155 76 106 57 45 4 225 234 80 148 127 244 134 53 253 63 46 94 181 107 98 89 184 171 37 37 24 202 164 253 25 124 129 175 90 251 160 213 226 235 127 199 196 212 57 84 237 222 161 18)))))

(deftest regression-false-hash
  (testing "false hash regression"
    (is (= (hash->unsigned-seq (edn-hash false))
           '(142 129 63 252 46 190 212 232 190 16 125 17 193 30 181 14 237 45 245 38 250 31 228 105 210 94 203 170 30 123 194 111 140 36 2 180 142 56 212 84 118 211 242 220 158 41 52 216 234 78 198 100 168 9 254 223 29 208 43 40 59 239 217 132)))))

(deftest regression-integer-hash
  (testing "Integer 42 hash regression"
    (is (= (hash->unsigned-seq (edn-hash 42))
           '(151 117 123 156 252 194 50 223 199 190 44 154 221 146 186 159 239 8 24 228 81 54 61 125 75 82 128 30 168 202 171 103 18 227 85 96 252 83 113 93 141 96 161 152 23 78 170 220 73 22 14 185 93 12 38 187 103 243 88 187 117 106 61 223)))))

(deftest regression-string-hash
  (testing "String hash regression"
    (is (= (hash->unsigned-seq (edn-hash "hello"))
           '(172 34 146 71 167 157 175 174 196 192 165 176 222 114 155 255 112 180 54 95 163 228 19 83 224 123 17 64 25 245 42 37 139 93 242 129 43 114 74 64 249 119 103 60 152 194 52 89 31 227 115 186 37 47 166 191 84 111 191 206 240 241 3 206)))))

(deftest regression-keyword-hash
  (testing "Keyword hash regression"
    (is (= (hash->unsigned-seq (edn-hash :core/test))
           '(81 242 197 27 192 87 35 56 18 156 60 12 19 246 195 133 87 60 105 30 108 13 203 127 18 211 230 68 94 45 82 27 78 206 36 120 59 156 155 27 209 94 21 172 76 105 193 165 65 227 119 73 95 160 188 239 241 236 133 29 165 108 103 180)))))

(deftest regression-uuid-val-hash
  (testing "UUID value hash regression"
    (is (= (hash->unsigned-seq (edn-hash #uuid "242525f1-8ed7-5979-9232-6992dd1e11e4"))
           '(80 95 137 152 30 171 19 193 162 196 226 16 117 43 186 153 158 242 222 31 244 228 237 162 3 163 234 243 176 83 151 16 86 82 110 114 133 88 212 158 9 234 6 255 5 28 37 7 145 182 162 249 106 195 144 97 230 242 251 211 226 217 199 120)))))

(deftest regression-vector-hash
  (testing "Vector hash regression"
    (is (= (hash->unsigned-seq (edn-hash [1 2 3 4]))
           '(114 221 240 13 191 92 71 26 56 120 90 127 183 207 30 4 174 23 12 38 76 0 99 213 77 111 222 104 70 172 213 245 135 177 236 65 16 220 197 54 255 86 16 241 188 173 194 147 179 194 223 21 40 12 29 37 139 208 38 158 33 40 23 111)))))

(deftest regression-map-hash
  (testing "Map hash regression - order independence verified"
    (let [expected '(183 104 137 31 115 191 38 111 189 194 176 45 225 86 89 244 166 237 164 146 236 39 56 81 13 80 199 180 106 92 165 70 230 215 248 67 162 229 141 75 4 167 231 192 149 7 199 33 236 85 234 122 193 199 85 230 48 46 3 32 174 219 30 120)]
      (is (= (hash->unsigned-seq (edn-hash {:a "hello" :balloon "world"})) expected))
      (is (= (hash->unsigned-seq (edn-hash {:balloon "world" :a "hello"})) expected)
          "Map hash must be order independent"))))

(deftest regression-set-hash
  (testing "Set hash regression"
    (is (= (hash->unsigned-seq (edn-hash #{1 2 3 4}))
           '(164 213 51 203 236 244 41 50 73 138 81 220 180 117 160 188 210 153 120 73 136 203 43 228 193 106 64 89 26 249 101 87 105 215 223 171 5 109 0 167 50 189 77 235 140 78 166 12 23 45 132 56 187 253 44 232 175 76 102 191 226 128 90 228)))))

(deftest regression-uuid-determinism
  (testing "UUID output regression"
    (is (= (str (uuid [1 2 3]))
           "01ae6768-cdcb-5944-afcc-718f2c0babcd"))))

;; =============================================================================
;; HashRef - transparent structural/merkle hashing (hasch.core only)
;; =============================================================================

(deftest hashref-transparency-test
  (testing "HashRef is transparent — same hash with or without"
    (let [subtree {:a 1 :b 2}
          ref (hc/hash-ref subtree)]
      (is (= (seq (hc/edn-hash [subtree]))
             (seq (hc/edn-hash [ref])))
          "Replacing a value with its HashRef must produce the same parent hash"))))

(deftest hashref-transparency-nested-test
  (testing "HashRef transparency works in nested structures"
    (let [inner [1 2 3]
          ref (hc/hash-ref inner)]
      (is (= (seq (hc/edn-hash {:data inner :meta "foo"}))
             (seq (hc/edn-hash {:data ref :meta "foo"})))
          "Nested HashRef must be transparent"))))

(deftest hashref-edn-hash-identity-test
  (testing "edn-hash of hash-ref equals edn-hash of original"
    (let [val {:name "Alice" :scores [1 2 3]}
          ref (hc/hash-ref val)]
      (is (= (seq (hc/edn-hash val))
             (seq (hc/edn-hash ref)))
          "(edn-hash (hash-ref x)) == (edn-hash x)"))))

(deftest hashref-uuid-identity-test
  (testing "uuid of hash-ref equals uuid of original"
    (let [val {:name "Alice" :scores [1 2 3]}
          ref (hc/hash-ref val)]
      (is (= (hc/uuid val)
             (hc/uuid ref))
          "(uuid (hash-ref x)) == (uuid x)"))))

(deftest hashref-structural-sharing-test
  (testing "Changing one subtree with HashRef changes root hash"
    (let [sub-a {:name "Alice" :scores [1 2 3]}
          sub-b {:name "Bob" :scores [4 5]}
          ref-a (hc/hash-ref sub-a)
          ref-b (hc/hash-ref sub-b)
          root1 (seq (hc/edn-hash {:users [ref-a ref-b]}))
          ;; Change sub-a, reuse ref-b
          sub-a' {:name "Alice" :scores [1 2 3 4]}
          ref-a' (hc/hash-ref sub-a')
          root2 (seq (hc/edn-hash {:users [ref-a' ref-b]}))]
      (is (not= root1 root2)
          "Root hash should change when a subtree changes"))))

;; =============================================================================
;; Record hashing with incognito
;; =============================================================================

(deftest record-hash-with-incognito-test
  (testing "Records hash deterministically via incognito"
    (is (= (hash->unsigned-seq (edn-hash (Bar. "hello")))
           (hash->unsigned-seq (edn-hash (Bar. "hello")))))))

(deftest record-hash-cross-platform-test
  (testing "Record hash matches incognito tagged literal"
    (let [record-hash (hash->unsigned-seq (edn-hash (Bar. "hello")))
          literal-hash (hash->unsigned-seq
                        (edn-hash (ic/map->IncognitoTaggedLiteral
                                   (ic/incognito-writer {} (Bar. "hello")))))]
      (is (= record-hash literal-hash)
          "Record and its incognito representation should hash identically"))))

#?(:cljs
   (defn ^:export run
     []
     (enable-console-print!)
     (run-tests)))

#?(:cljs
   (do
     (run)
     (when (exists? js/phantom)
       (js/phantom.exit))))
