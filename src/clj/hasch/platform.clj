(ns hasch.platform
  "Platform specific implementations."
  (:require [hasch.benc :refer [magics IHashCoercion -coerce split-size max-entropy]]
            [clojure.edn :as edn])
  (:import java.security.MessageDigest
           java.nio.ByteBuffer
           java.io.ByteArrayOutputStream))

(set! *warn-on-reflection* true)

(defn as-value ;; TODO maybe use transit json in memory?
  "Transforms runtime specific records by printing and reading with a default tagged reader.
This is hence is as slow as pr-str and read-string."
  [v]
  (edn/read-string {:default (fn [tag val] [tag val])}
                   (pr-str v)))

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

(let [byte-class (Class/forName "[B")]
  (defn- ^bytes digest
    [^MessageDigest md bytes-or-seq-of-bytes]
    (.reset md)
    (if (= (type bytes-or-seq-of-bytes) byte-class)
      (.update md ^bytes bytes-or-seq-of-bytes)
      (doseq [^bytes bs bytes-or-seq-of-bytes]
        (.update md bs)))
    (.digest md)))

(defn uuid5
  "Generates a UUID version 5 from a sha-1 hash byte sequence.
Our hash version is coded in first 2 bits."
  [^bytes sha-hash]
  (let [bb (ByteBuffer/wrap sha-hash)
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

(defn ^bytes coerce-seq [^MessageDigest resetable-md md-create-fn seq]
  (.reset resetable-md)
  (loop [s seq]
    (let [f (first s)]
      (when f
        (.update resetable-md ^bytes (-coerce f resetable-md md-create-fn))
        (recur (rest s)))))
  (.digest resetable-md))

(defn ^bytes padded-coerce
  "Commutatively coerces elements of collection, seq entries should be crypto hashed
  to avoid collisions in XOR."
  [seq resetable-md md-create-fn]
  (let [len (min (long (alength ^bytes (first seq))) max-entropy)]
    (reduce (fn padded-xor [^bytes acc ^bytes elem]
              (loop [i 0]
                (when (< i len)
                  (aset acc i (byte (bit-xor (aget acc i) (aget elem i))))
                  (recur (inc i))))
              acc)
            (byte-array len)
            seq)))

(defn ^bytes encode [^Byte magic ^bytes a]
  (let [out (ByteArrayOutputStream.)]
    (.write out (byte-array 1 magic))
    (.write out a)
    (.toByteArray out)))


(defn ^bytes encode-safe [^MessageDigest resetable-md ^bytes a]
  (if (< (count a) split-size)
    (let [len (long (alength a))
          ea (byte-array len)]
      (loop [i 0]
        (when-not (= i len)
          (let [e (aget a i)]
            (when (and (> e (byte 0))
                       (< e (byte 30)))
              (aset ea i (byte 1))))
          (recur (inc i))))
      (let [out (ByteArrayOutputStream.)]
        (.write out a)
        (.write out ea)
        (.toByteArray out)))
    (digest resetable-md a)))

(defn- ^bytes str->utf8 [x]
  (-> x str (.getBytes "UTF-8")))

(extend-protocol IHashCoercion
  java.lang.Boolean
  (-coerce [this resetable-md md-create-fn]
    (encode (:boolean magics) (byte-array 1 (if this (byte 41) (byte 40)))))

  ;; don't distinguish characters from string for javascript
  java.lang.Character
  (-coerce [this resetable-md md-create-fn]
    (encode (:string magics) (encode-safe resetable-md (str->utf8 this))))

  java.lang.String
  (-coerce [this resetable-md md-create-fn]
    (encode (:string magics) (encode-safe resetable-md (str->utf8 this))))

  java.lang.Integer
  (-coerce [this resetable-md md-create-fn]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.lang.Long
  (-coerce [this resetable-md md-create-fn]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.lang.Float
  (-coerce [this resetable-md md-create-fn]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.lang.Double
  (-coerce [this resetable-md md-create-fn]
    (encode (:number magics) (.getBytes (.toString this) "UTF-8")))

  java.util.UUID
  (-coerce [this resetable-md md-create-fn]
    (encode (:uuid magics) (.getBytes (.toString this) "UTF-8")))

  java.util.Date
  (-coerce [this resetable-md md-create-fn]
    (encode (:inst magics) (.getBytes (.toString ^java.lang.Long (.getTime this)) "UTF-8")))

  nil
  (-coerce [this resetable-md md-create-fn]
    (encode (:nil magics) (byte-array 0)))

  clojure.lang.Symbol
  (-coerce [this resetable-md md-create-fn]
    (encode (:symbol magics) (encode-safe resetable-md (str->utf8 this))))

  clojure.lang.Keyword
  (-coerce [this resetable-md md-create-fn]
    (encode (:keyword magics) (encode-safe resetable-md (str->utf8 this))))

  clojure.lang.ISeq
  (-coerce [this resetable-md md-create-fn]
    (encode (:seq magics) (coerce-seq resetable-md md-create-fn this)))

  clojure.lang.IPersistentVector
  (-coerce [this resetable-md md-create-fn]
    (encode (:vector magics) (coerce-seq resetable-md md-create-fn this)))

  clojure.lang.IPersistentMap
  (-coerce [this ^MessageDigest resetable-md md-create-fn]
    (if (record? this)
      (let [[tag val] (as-value this)]
        (encode (:literal magics)
                (coerce-seq resetable-md
                            md-create-fn
                            [(-coerce tag resetable-md md-create-fn)
                             (-coerce val resetable-md md-create-fn)])))
      (encode (:map magics)
              (digest resetable-md
                      (padded-coerce (map #(-coerce %
                                                    resetable-md
                                                    md-create-fn)
                                          (seq this))
                                     resetable-md
                                     md-create-fn)))))



  clojure.lang.IPersistentSet
  (-coerce [this ^MessageDigest resetable-md md-create-fn]
    (encode (:set magics)
            (digest resetable-md
                    (padded-coerce (map #(digest resetable-md
                                                 (-coerce % resetable-md md-create-fn))
                                        (seq this))
                                   resetable-md
                                   md-create-fn))))

  java.lang.Object ;; for custom deftypes that might be printable (but generally not)
  (-coerce [this resetable-md md-create-fn]
    (let [[tag val] (as-value this)]
      (encode (:literal magics)
              (coerce-seq resetable-md
                          md-create-fn
                          [(-coerce tag resetable-md md-create-fn)
                           (-coerce val resetable-md md-create-fn)])))))


(extend (Class/forName "[B")
  IHashCoercion
  {:-coerce (fn [^bytes this resetable-md md-create-fn]
              (encode (:binary magics) (encode-safe resetable-md this)))})




(comment
  (map byte (-coerce {:hello :world :foo :bar 1 2} (sha512-message-digest) sha512-message-digest))

  (map byte (-coerce #{1 2 3} (sha512-message-digest) sha512-message-digest))

  (use 'criterium.core)

  (def million-map (into {} (doall (map vec (partition 2 ; 2.40 secs
                                                       (interleave (range 1000000)
                                                                   (range 1000000)))))))

  (def million-seq (doall (map vec (partition 2 ;; 1.8 secs
                                              (interleave (range 1000000)
                                                          (range 1000000 2000000))))))

  (def million-seq2 (doall (range 1000000))) ;; 275 ms

  (take 10 (time (into (sorted-set) (range 1e6)))) ;; 2.3 s

  (bench (coerce-seq (sha512-message-digest) sha512-message-digest ;; 15.6 ms
                     (seq (into (sorted-set) (range 1e4)))))

  (bench (-coerce (into #{} (range 1e4)) (sha512-message-digest) sha512-message-digest)) ;; 19.6 ms

  (bench (-coerce (seq (into (sorted-set) (range 10)))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 8.16 us

  (bench (-coerce (into #{} (range 10))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 22.2 us

  (bench (-coerce (seq (into (sorted-set) (range 100)))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 88 us

  (bench (-coerce (into #{} (range 100))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 192 us


  (bench (-coerce (seq (into (sorted-set) (range 1e4)))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 15.6 ms

  (bench (-coerce (into #{} (range 1e4))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 19.7 ms

  (bench (-coerce (seq (into (sorted-map) (map vec (partition 2 (range 10)))))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 19.8 us


  (bench (-coerce (into {} (map vec (partition 2 (range 10))))
                  (sha512-message-digest)
                  sha512-message-digest)) ;; 23.5 us

  (def million-set (doall (into #{} (range 1000000)))) ;; 1.85 secs

  (bench (-coerce million-set (sha512-message-digest) sha512-message-digest))

  (def million-seq3 (doall (repeat 1000000 "hello world"))) ;; 774 msecs

  (bench (-coerce million-seq3 (sha512-message-digest) sha512-message-digest))

  (def million-seq4 (doall (repeat 1000000 :foo/bar))) ;; 607 msecs

  (bench (-coerce million-seq4 (sha512-message-digest) sha512-message-digest))

  (def datom-vector (doall (vec (repeat 10000 {:db/id 18239
                                               :person/name "Frederic"
                                               :person/familyname "Johanson"
                                               :person/street "Fifty-First Street 53"
                                               :person/postal 38237
                                               :person/phone "02343248474"
                                               :person/weight 38.23}))))

  (time (-coerce datom-vector (sha512-message-digest) sha512-message-digest))
  (bench (-coerce datom-vector (sha512-message-digest) sha512-message-digest)) ;; 246 ms

  (time (r/fold (fn ([] (byte-array 128))
                  ([^bytes acc ^bytes elem]
                   (loop [i 0]
                     (when (< i 128)
                       (aset acc i (byte (bit-xor (aget acc i) (aget elem i))))
                       (recur (inc i))))
                   acc))
                (fn [_ k v]
                  (-coerce [k v] (sha512-message-digest) sha512-message-digest))
                million-map))





  ;; if not single or few byte values, but at least 8 byte size factor per item ~12x
  ;; factor for single byte ~100x
  (def bs (apply concat (repeat 100000 (.getBytes "Hello World!"))))
  (def barr #_(byte-array bs) (byte-array (* 1024 1024 300) (byte 42)))
  (def barrs (doall (take (* 1024 1024 10) (repeat (byte-array 1 (byte 42))))
                    #_(map byte-array (partition 1 barr))))

  (bench (-coerce barr (sha512-message-digest) sha512-message-digest)) ;; 1.99 secs


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
