(ns hasch.platform
  "Platform specific implementations."
  (:require [hasch.benc :refer [magics IHashCoercion -coerce]]
            [clojure.edn :as edn])
  (:import java.security.MessageDigest
           java.nio.ByteBuffer
           java.io.ByteArrayOutputStream))

(set! *warn-on-reflection* true)

(def ^:dynamic *resetable-digest* nil)


(defn as-value ;; TODO maybe use transit json in memory?
  "Transforms runtime specific records by printing and reading with a default tagged reader."
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

(defn ^bytes encode [^Byte magic ^bytes a]
  (let [len (long (alength a))
        ea (byte-array (inc (* len 2)))]
    (aset ea 0 magic)
    (loop [i 0]
      (when-not (= i len)
        (let [e (aget a i)]
          (aset ea (inc (* i 2)) e)
          (when (and (> e (byte 0))
                     (< e (byte 30)))
            (aset ea (+ (* i 2) 2) e)))
      (recur (inc i))))
    ea))

(defn sha512-message-digest []
  (MessageDigest/getInstance "sha-512"))

(let [byte-class (Class/forName "[B")]
  (defn- digest
    [^MessageDigest md bytes-or-seq-of-bytes]
    (.reset md)
    (if (= (type bytes-or-seq-of-bytes) byte-class)
      (.update md ^bytes bytes-or-seq-of-bytes)
      (doseq [^bytes bs bytes-or-seq-of-bytes]
        (.update md bs)))
    (.digest md)))

(defn- bytes->long [bytes] ;endianness?
  (->> bytes
       (into-array Byte/TYPE)
       ByteBuffer/wrap
       .getLong))


(defn uuid5
  "Generates a UUID version 5 from a sha-1 hash byte sequence.
Our hash version is coded in first 2 bits."
  [sha-hash]
  (let [high  (take 8 sha-hash)
        low (->> sha-hash (drop 8) (take 8))]
    (java.util.UUID. (-> (bytes->long high)
                         (bit-or 0x0000000000005000)
                         (bit-and 0x7fffffffffff5fff)
                         (bit-clear 63) ;; needed because of BigInt cast of bitmask
                         (bit-clear 62))
                     (-> (bytes->long low)
                         (bit-set 63)
                         (bit-clear 62)))))

(defn coerce-seq [^MessageDigest resetable-md md-create-fn seq]
  (.reset resetable-md)
  (loop [s seq]
    (let [f (first s)]
      (when f
        (.update resetable-md ^bytes (-coerce f resetable-md md-create-fn))
        (recur (rest s)))))
  (.digest resetable-md))

(defn encode-symbolic [^Byte token symbolic]
  (let [out (ByteArrayOutputStream.)]
    (.write out (.getBytes (or (namespace symbolic) "") "UTF-8"))
    (.write out (byte-array 1 token))
    (.write out (.getBytes (or (name symbolic) "") "UTF-8"))
    (encode token (.toByteArray out))))


(defn encode-number
  "Assumption: Numbers cannot be serialized to strings with ASCII bytes 0 < b < 30.
   This spares ~2x for number encoding."
  [^Byte token number]
  (let [out (ByteArrayOutputStream.)]
    (.write out (byte-array 1 token))
    (.write out (.getBytes (str number) "UTF-8"))
    (.toByteArray out)))

(defn encode-binary [sha512-unsigned-hash-bytes-digest]
  "Encode an external binary hash."
  (encode (:binary magics) sha512-unsigned-hash-bytes-digest))

(defn padded-coerce
  "Commutatively coerces elements of collection, padding ensures all bits
are included in the hash."
  [seq resetable-md md-create-fn]
  (let [len (count (first seq))]
    (reduce (fn padded-xor [^bytes acc ^bytes elem]
              (loop [i 0]
                (when (< i len)
                  (aset acc i (byte (bit-xor (aget acc i) (aget elem i))))
                  (recur (inc i))))
              acc)
            (byte-array len)
            seq)))

(def split-size 1024)

(extend-protocol IHashCoercion
  java.lang.Boolean
  (-coerce [this resetable-md md-create-fn]
    (let [barr (byte-array 2)]
      (aset barr 0 ^Byte (:boolean magics))
      (aset barr 1 (if this (byte 1) (byte 0)))))

  ;; don't distinguish characters from string for javascript
  java.lang.Character
  (-coerce [this resetable-md md-create-fn] (encode (:string magics) (.getBytes (str this) "UTF-8")))

  java.lang.String
  (-coerce [this resetable-md md-create-fn]
    (let [bs (.getBytes this "UTF-8")]
      (encode (:string magics) (if (< (alength bs) split-size)
                                 bs
                                 (digest resetable-md bs)))))

  java.lang.Integer
  (-coerce [this resetable-md md-create-fn] (encode-number (:number magics) this))

  java.lang.Long
  (-coerce [this resetable-md md-create-fn] (encode-number (:number magics) this))

  java.lang.Float
  (-coerce [this resetable-md md-create-fn] (encode-number (:number magics) this))

  java.lang.Double
  (-coerce [this resetable-md md-create-fn] (encode-number (:number magics) this))

  java.util.UUID
  (-coerce [this resetable-md md-create-fn] (encode (:uuid magics) (.getBytes (str this) "UTF-8")))

  java.util.Date
  (-coerce [this resetable-md md-create-fn] (encode (:inst magics) (.getBytes (str (.getTime this)))))

  nil
  (-coerce [this resetable-md md-create-fn] (encode (:nil magics) (byte-array 0)))

  clojure.lang.Symbol
  (-coerce [this resetable-md md-create-fn]
    (encode-symbolic (:symbol magics) this))

  clojure.lang.Keyword
  (-coerce [this resetable-md md-create-fn]
    (encode-symbolic (:keyword magics) this))

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
                (coerce-seq resetable-md md-create-fn [(-coerce tag resetable-md md-create-fn)
                                                       (byte-array 1 (:literal magics))
                                                       (-coerce val resetable-md md-create-fn)])))
      (encode (:map magics)
              (do
                (.reset resetable-md)
                ;; TODO do not map over kv-entries as vectors?
                (.update resetable-md ^bytes (padded-coerce (map #(-coerce % resetable-md md-create-fn)
                                                                 (seq this))
                                                            resetable-md
                                                            md-create-fn))
                (.digest resetable-md)))))



  clojure.lang.IPersistentSet
  (-coerce [this ^MessageDigest resetable-md md-create-fn]
    (encode (:set magics)
            (do
              (.reset resetable-md)
              (.update resetable-md ^bytes (padded-coerce (map #(digest resetable-md (-coerce % resetable-md md-create-fn))
                                                               (seq this))
                                                          resetable-md
                                                          md-create-fn))
              (.digest resetable-md))))

  java.lang.Object
  (-coerce [this resetable-md md-create-fn]
    (let [[tag val] (as-value this)]
      (let [out (ByteArrayOutputStream.)]
        (.write out ^bytes (-coerce tag resetable-md md-create-fn))
        (.write out (byte-array 1 (:literal magics)))
        (.write out ^bytes (-coerce val resetable-md md-create-fn))
        (encode (:literal magics) (.toByteArray out))))))


(extend (Class/forName "[B")
  IHashCoercion
  {:-coerce (fn [^bytes this resetable-md md-create-fn]
              (encode-binary (digest resetable-md this)))})






(comment
  (map byte (-coerce {:hello :world :foo :bar 1 2} (sha512-message-digest) sha512-message-digest))

  (map byte (-coerce #{1 2 3} (sha512-message-digest) sha512-message-digest))

  (def million-map (into {} (doall (map vec (partition 2 ; 49 secs
                                                       (interleave (range 2000000)
                                                                   (range 2000000 4000000)))))))

  (def million-seq (doall (map vec (partition 2 ;; 15 secs
                                              (interleave (range 1000000)
                                                          (range 1000000 2000000))))))

  (def million-seq2 (doall (range 1000000))) ;; 520 ms

  (def million-set (doall (into #{} (range 1000000)))) ;; 5 secs

  (def million-seq3 (doall (repeat 1000000 "hello"))) ;; 900 ms

  (def million-seq4 (doall (repeat 1000000 :foo))) ;; 1 s

  (def million-seq4 (doall (repeat 1000000 :foo/bar))) ;; 1.5 s

  (let [;million-words (doall (repeat 1000000 "hello"))
        resetable-md (sha512-message-digest)]
    (time (-coerce million-map resetable-md sha512-message-digest)) ;; 14 secs
    #_(time (hash-imap million-map) ) ;; 3 secs
    )

  (defn hash-combine [seed hash]
                                        ; a la boost
    (bit-xor seed (+ hash 0x9e3779b9
                     (bit-shift-left seed 6)
                     (bit-shift-right seed 2))))

  (defn- hash-coll [coll]
    (if (seq coll)
      (loop [res (hash (first coll)) s (next coll)]
        (if (nil? s)
          res
          (recur (hash-combine res (hash (first s))) (next s))))
      0))

  (defn- hash-imap [m]
    ;; a la clojure.lang.APersistentMap
    (loop [h 0 s (seq m)]
      (if s
        (let [e (first s)]
          (recur (mod (+ h (bit-xor (hash (key e)) (hash (val e))))
                      4503599627370496)
                 (next s)))
        h)))

  (= (hash-imap {:hello :world})
     (hash-imap {:world :hello}))

  ;; if not single or few byte values, but at least 8 byte size factor per item ~12x
  ;; factor for single byte ~100x
  (def bs (apply concat (repeat 100000 (.getBytes "Hello World!"))))
  (def barr #_(byte-array bs) (byte-array (* 1024 1024 100) (byte 42)))
  (def barrs (doall (take (* 1024 1024 10) (repeat (byte-array 1 (byte 42))))
                    #_(map byte-array (partition 1 barr))))
  (let []
    (time (-coerce barr (sha512-message-digest) sha512-message-digest))  ;; 882 msecs
    #_(let [md (MessageDigest/getInstance "sha-1")] ;; 350 msecs
      (time (do (.update md barr)
                (.digest md))))
    #_(let [md (MessageDigest/getInstance "sha-1")]
        (time (doall (map (fn [^bytes barr]
                            (.update md barr)
                            (.digest md)) barrs))))
    #_(let [md (MessageDigest/getInstance "sha-1")] ;; 32 secs without encoding, 50 secs with
        (time (doall (map (fn [^bytes barr]
                            (let [len (count barr)
                                  barr2 (byte-array (* len 2))]
                              (loop [i 0]
                                (when-not (= i len)
                                  (let [e (aget barr i)]
                                    (aset barr2 (* i 2) e)
                                    (when (and (> e (byte 0))
                                               (< e (byte 30)))
                                      (aset barr2 (inc (* i 2)) e)))
                                  (recur (inc i))))
                              (.update md barr2))) barrs)))
        (println (map byte (time (.digest md)))))
    #_(let [md (MessageDigest/getInstance "sha-1")] ;; 32 secs without encoding, 50 secs with
      (time (doall (map #(.update md (encode (:binary magics) %)) barrs)))
      (println (map byte (time (.digest md)))))
    #_(time (hash-coll barrs)) ;; 20 secs
    nil)

                                        ;  [<0> <<>>, <7> <<117>>] ;; binary + integer bytes
                                        ;    0,       7, 1,1,7,1,1,7
                                        ;  [<0> <<7 1 1>>>]          ;; binary collision
                                        ;    0,       7, 1,1,7,1,1

  ;; prehash map entries to make xor less collisionary
  ;; padded-coerce with mapping over entries as vectors with coerce should be fine
  ;; but not as cheap as on seqs


  (def arr (into-array Byte/TYPE (take (* 1024) (repeatedly #(- (rand-int 256) 128)))))


  )
