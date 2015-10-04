(ns hasch.platform
  "Platform specific implementations."
  (:require [hasch.benc :refer [split-size encode-safe]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [incognito.base :as ib]
            [hasch.benc :refer [magics PHashCoercion -coerce
                                digest coerce-seq xor-hashes encode-safe]])
  (:import java.io.ByteArrayOutputStream
           java.nio.ByteBuffer
           java.security.MessageDigest))


(set! *warn-on-reflection* true)

(defn uuid4
  "Generates a UUID version 4 (random)."
  []
  (java.util.UUID/randomUUID))

(defn byte->hex [b]
  (-> b
      (bit-and 0xff)
      (+ 0x100)
      (Integer/toString 16)
      (.substring 1)))

(defn hash->str [bytes]
  (apply str (map byte->hex bytes)))

(defn ^MessageDigest sha512-message-digest []
  (MessageDigest/getInstance "sha-512"))

(defn ^MessageDigest md5-message-digest []
  (MessageDigest/getInstance "md5"))

(defn uuid5
  "Generates a UUID version 5 from a sha-1 hash byte sequence.
Our hash version is coded in first 2 bits."
  [sha-hash]
  (let [bb (ByteBuffer/wrap (byte-array sha-hash))
        high (.getLong bb)
        low (.getLong bb)]
    (java.util.UUID. (-> high
                         (bit-or 0x0000000000005000)
                         (bit-and 0x7fffffffffff5fff)
                         (bit-clear 63) ;; needed because of BigInt cast of bitmask
                         (bit-clear 62))
                     (-> low
                         (bit-set 63)
                         (bit-clear 62)))))

(defn ^bytes encode [^Byte magic ^bytes a]
  (let [out (ByteArrayOutputStream.)]
    (.write out (byte-array 1 magic))
    (.write out a)
    (.toByteArray out)))

(defn- ^bytes str->utf8 [x]
  (-> x str (.getBytes "UTF-8")))

(extend-protocol PHashCoercion
  java.lang.Boolean
  (-coerce [this md-create-fn write-handlers]
    (encode (:boolean magics) (byte-array 1 (if this (byte 41) (byte 40)))))

  ;; don't distinguish characters from string for javascript
  java.lang.Character
  (-coerce [this md-create-fn write-handlers]
    (encode (:string magics) (encode-safe (str->utf8 this) md-create-fn)))

  java.lang.String
  (-coerce [this md-create-fn write-handlers]
    (encode (:string magics) (encode-safe (str->utf8 this) md-create-fn)))

  java.lang.Integer
  (-coerce [this md-create-fn write-handlers]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.lang.Long
  (-coerce [this md-create-fn write-handlers]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.lang.Float
  (-coerce [this md-create-fn write-handlers]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.lang.Double
  (-coerce [this md-create-fn write-handlers]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.util.UUID
  (-coerce [this md-create-fn write-handlers]
    (encode (:uuid magics) (.getBytes (.toString this) "UTF-8")))

  java.util.Date
  (-coerce [this md-create-fn write-handlers]
    (encode (:inst magics) (.getBytes (.toString ^java.lang.Long (.getTime this)) "UTF-8")))

  nil
  (-coerce [this md-create-fn write-handlers]
    (encode (:nil magics) (byte-array 0)))

  clojure.lang.Symbol
  (-coerce [this md-create-fn write-handlers]
    (encode (:symbol magics) (encode-safe (str->utf8 this) md-create-fn)))

  clojure.lang.Keyword
  (-coerce [this md-create-fn write-handlers]
    (encode (:keyword magics) (encode-safe (str->utf8 this) md-create-fn)))

  clojure.lang.ISeq
  (-coerce [this md-create-fn write-handlers]
    (encode (:seq magics) (coerce-seq this md-create-fn write-handlers)))

  clojure.lang.IPersistentVector
  (-coerce [this md-create-fn write-handlers]
    (encode (:vector magics) (coerce-seq this md-create-fn write-handlers)))

  incognito.base.IncognitoTaggedLiteral
  (-coerce [this md-create-fn write-handlers]
    (let [{:keys [tag value]} this]
      (encode (:literal magics) (coerce-seq [tag value] md-create-fn write-handlers))))

  clojure.lang.IRecord
  (-coerce [this md-create-fn write-handlers]
    (let [{:keys [tag value]} (ib/incognito-writer write-handlers this)]
      (encode (:literal magics) (coerce-seq [tag value] md-create-fn write-handlers))))

  clojure.lang.IPersistentMap
  (-coerce [this md-create-fn write-handlers]
    (encode (:map magics) (xor-hashes (map #(-coerce % md-create-fn write-handlers) (seq this)))))


  clojure.lang.IPersistentSet
  (-coerce [this md-create-fn write-handlers]
    (encode (:set magics) (xor-hashes (map #(digest (-coerce % md-create-fn write-handlers)
                                                    md-create-fn)
                                           (seq this)))))

  ;; not ideal, InputStream might be more flexible
  ;; file is used due to length knowledge
  java.io.File
  (-coerce [f md-create-fn write-handlers]
    (let [^MessageDigest md (md-create-fn)
          len (.length f)]
      (with-open [fis (java.io.FileInputStream. f)]
        (encode (:binary magics)
                ;; support default split-size behaviour transparently
                (if (< len split-size)
                  (let [ba (with-open [out (java.io.ByteArrayOutputStream.)]
                             (clojure.java.io/copy fis out)
                             (.toByteArray out))]
                    (encode-safe ba md-create-fn))
                  (let [ba (byte-array (* 1024 1024))]
                    (loop [size (.read fis ba)]
                      (if (neg? size) (.digest md)
                          (do
                            (.update md ba 0 size)
                            (recur (.read fis ba))))))))))))


(extend (Class/forName "[B")
  PHashCoercion
  {:-coerce (fn [^bytes this md-create-fn write-handlers]
              (encode (:binary magics) (encode-safe this md-create-fn)))})




(comment
  (require '[clojure.java.io :as io])
  (def foo (io/file "/tmp/foo"))
  (.length foo)

  (defn slurp-bytes
    "Slurp the bytes from a slurpable thing"
    [x]
    (with-open [out (java.io.ByteArrayOutputStream.)]
      (clojure.java.io/copy (clojure.java.io/input-stream x) out)
      (.toByteArray out)))

  (clojure.reflect/reflect foo)
  (= (map byte (-coerce (io/file "/tmp/bar") sha512-message-digest))
     (map byte (-coerce (slurp-bytes "/tmp/bar") sha512-message-digest)))


  (map byte (-coerce {:hello :world :foo :bar 1 2} sha512-message-digest))

  (map byte (-coerce #{1 2 3} sha512-message-digest))

  (use 'criterium.core)

  (def million-map (into {} (doall (map vec (partition 2
                                                       (interleave (range 1000000)
                                                                   (range 1000000)))))))

  (bench (-coerce million-map sha512-message-digest)) ;; 3.80 secs

  (def million-seq (doall (map vec (partition 2
                                              (interleave (range 1000000)
                                                          (range 1000000 2000000))))))

  (def million-seq2 (doall (range 1000000)))

  (bench (-coerce million-seq2 sha512-message-digest)) ;; 296 ms

  (bench (-coerce million-seq2 md5-message-digest))

  (take 10 (time (into (sorted-set) (range 1e6)))) ;; 1.7 s


  (bench (coerce-seq sha512-message-digest (seq (into (sorted-set) (range 1e4)))))

  (bench (-coerce (into #{} (range 1e4)) sha512-message-digest))


  (bench (-coerce (seq (into (sorted-set) (range 10))) sha512-message-digest)) ;; 8.6 us

  (bench (-coerce (into #{} (range 10)) sha512-message-digest)) ;; 31.7 us


  (bench (-coerce (seq (into (sorted-set) (range 100))) sha512-message-digest))

  (bench (-coerce (into #{} (range 100)) sha512-message-digest))


  (bench (-coerce (seq (into (sorted-set) (range 1e4))) sha512-message-digest))

  (bench (-coerce (into #{} (range 1e4)) sha512-message-digest))


  (def small-map (into {} (map vec (partition 2 (take 10 (repeatedly rand))))))
  (bench (-coerce (apply concat (seq (into (sorted-map) small-map)))
                  sha512-message-digest)) ;; 12.1 us

  (bench (-coerce small-map sha512-message-digest)) ;; 20.7 us


  (def medium-map (into {} (map vec (partition 2 (take 2e6 (repeatedly rand))))))
  (bench (-coerce (apply concat (seq (into (sorted-map) medium-map)))
                  sha512-message-digest))

  (bench (-coerce medium-map sha512-message-digest))



  (def million-set (doall (into #{} (range 1000000))))

  (bench (-coerce million-set sha512-message-digest)) ;; 2.69 secs


  (def million-seq3 (doall (repeat 1000000 "hello world")))

  (bench (-coerce million-seq3 sha512-message-digest)) ;; 916 msecs


  (def million-seq4 (doall (repeat 1000000 :foo/bar)))

  (bench (-coerce million-seq4 sha512-message-digest)) ;; 752 msecs


  (def datom-vector (doall (vec (repeat 10000 {:db/id 18239
                                               :person/name "Frederic"
                                               :person/familyname "Johanson"
                                               :person/street "Fifty-First Street 53"
                                               :person/postal 38237
                                               :person/phone "02343248474"
                                               :person/weight 38.23}))))
  (let [val (doall (vec (repeat 10000 {:db/id 18239
                                       :person/name "Frederic"
                                       :person/familyname "Johanson"
                                       :person/street "Fifty-First Street 53"
                                       :person/postal 38237
                                       :person/phone "02343248474"
                                       :person/weight 38.23})))]
    (bench (-coerce val sha512-message-digest)))

  (time (-coerce datom-vector sha512-message-digest))
  (bench (-coerce datom-vector sha512-message-digest)) ;; xor: 316 ms, sort: 207 ms

  ;; if not single or few byte values, but at least 8 byte size factor per item ~12x
  ;; factor for single byte ~100x
  (def bs (apply concat (repeat 100000 (.getBytes "Hello World!"))))
  (def barr #_(byte-array bs) (byte-array (* 1024 1024 300) (byte 42)))
  (def barrs (doall (take (* 1024 1024 10) (repeat (byte-array 1 (byte 42))))
                    #_(map byte-array (partition 1 barr))))

  (bench (-coerce barr sha512-message-digest)) ;; 1.99 secs


  (def arr (into-array Byte/TYPE (take (* 1024) (repeatedly #(- (rand-int 256) 128)))))


  ;; hasch 0.2.3
  (use 'criterium.core)

  (def million-map (into {} (doall (map vec (partition 2
                                                       (interleave (range 1000000)
                                                                   (range 1000000 2000000)))))))

  (bench (uuid million-map)) ;; 27 secs

  (def million-seq3 (doall (repeat 1000000 "hello world")))

  (bench (uuid million-seq3)) ;; 16 secs


  (def datom-vector (doall (vec (repeat 10000 {:db/id 18239
                                               :person/name "Frederic"
                                               :person/familyname "Johanson"
                                               :person/street "Fifty-First Street 53"
                                               :person/postal 38237
                                               :person/telefon "02343248474"
                                               :person/weeight 0.3823}))))

  (bench (uuid datom-vector)) ;; 2.6 secs


  )
