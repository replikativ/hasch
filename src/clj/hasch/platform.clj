(ns hasch.platform
  "Platform specific implementations."
  (:require [hasch.benc :refer :all])
  (:import java.security.MessageDigest
           java.nio.ByteBuffer))


(defn byte->hex [b]
  (-> b
      (bit-and 0xff)
      (+ 0x100)
      (Integer/toString 16)
      (.substring 1)))


(defn hash->str [bytes]
  (apply str (map byte->hex bytes)))


(defn sha-1 [bytes]
  (let [md (MessageDigest/getInstance "sha-1")
        sarr (into-array Byte/TYPE bytes)]
    (.update md sarr)
    (map byte (.digest md))))


(defn- bytes->long [bytes] ;endianness?
  (->> bytes
       (into-array Byte/TYPE)
       ByteBuffer/wrap
       .getLong))


(defn uuid5
  "Generates a uuid5 from a sha-1 hash byte sequence.
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

(mapcat benc (.getBytes "hällo" "UTF-8"))

(extend-protocol IHashCoercion
  java.lang.Boolean
  (-coerce [this hash-fn] (list (:boolean magics) (if this 1 0)))

  ;; don't distinguish characters from string for javascript
  java.lang.Character
  (-coerce [this hash-fn] (conj (mapcat benc (map byte (.getBytes (str this) "UTF-8")))
                                (:string magics)))

  java.lang.String
  (-coerce [this hash-fn] (conj (mapcat benc (map byte (.getBytes this "UTF-8")))
                                (:string magics)))

  java.lang.Integer
  (-coerce [this hash-fn] (conj (mapcat benc (str this))
                                (:number magics)))

  java.lang.Long
  (-coerce [this hash-fn] (conj (mapcat benc (str this))
                                (:number magics)))

  java.lang.Float
  (-coerce [this hash-fn] (conj (mapcat benc (str this))
                                (:number magics)))

  java.lang.Double
  (-coerce [this hash-fn] (conj (mapcat benc (str this))
                                (:number magics)))

  java.util.UUID
  (-coerce [this hash-fn] (conj (mapcat benc (str this))
                                (:uuid magics)))

  java.util.Date
  (-coerce [this hash-fn] (conj (mapcat benc (str (.getTime this)))
                                (:inst magics)))

  nil
  (-coerce [this hash-fn] (list (:nil magics)))

  clojure.lang.Symbol
  (-coerce [this hash-fn] (conj (mapcat benc
                                        (concat (namespace this)
                                                (name this)))
                                (:symbol magics)))

  clojure.lang.Keyword
  (-coerce [this hash-fn] (conj (mapcat benc
                                        (concat (namespace this)
                                                (name this)))
                                (:keyword magics)))

  clojure.lang.ISeq
  (-coerce [this hash-fn] (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                                         (:seq magics))))

  clojure.lang.IPersistentVector
  (-coerce [this hash-fn] (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                                         (:vector magics))))

  clojure.lang.IPersistentMap
  (-coerce [this hash-fn] (hash-fn (conj (padded-coerce this hash-fn)
                                         (:map magics))))

  clojure.lang.IPersistentSet
  (-coerce [this hash-fn] (hash-fn (conj (padded-coerce this hash-fn)
                                         (:set magics)))))

(defn boolean? [val]
  (= (type val) java.lang.Boolean))


(defn uuid? [val]
  (= (type val) java.util.UUID))


(defn date? [val]
  (= (type val) java.util.Date))
