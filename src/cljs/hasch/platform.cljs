(ns hasch.platform
  (:require [goog.crypt.Sha512]
            [cljs.reader :as reader]
            [clojure.string]
            [hasch.benc :refer [IHashCoercion -coerce magics split-size]]))

#_(do
    (ns dev)
    (def repl-env (reset! cemerick.austin.repls/browser-repl-env
                         (cemerick.austin/repl-env)))
    (cemerick.austin.repls/cljs-repl repl-env))

(defn as-value
  "Transforms runtime specific records by printing and reading with a default tagged reader."
  [v]
  (binding [reader/*tag-table* (atom (select-keys @reader/*tag-table*
                                                  #{"inst" "uuid" "queue"}))
            reader/*default-data-reader-fn*
            (atom (fn [tag val] [tag val]))]
    (reader/read-string (pr-str v))))


;; taken from https://github.com/whodidthis/cljs-uuid-utils/blob/master/src/cljs_uuid_utils.cljs
;; Copyright (C) 2012 Frank Siebenlist
;; Distributed under the Eclipse Public License, the same as Clojure.
;; TODO check: might not have enough randomness in some browsers (?)
(defn uuid4
  "(uuid4) => new-uuid
   Arguments and Values:
   new-uuid --- new type 4 (pseudo randomly generated) cljs.core/UUID instance.
   Description:
   Returns pseudo randomly generated UUID,
   like: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx as per http://www.ietf.org/rfc/rfc4122.txt.
   Examples:
   (uuid4) => #uuid \"305e764d-b451-47ae-a90d-5db782ac1f2e\"
   (type (uuid4)) => cljs.core/UUID"
  []
  (letfn [(f [] (.toString (rand-int 16) 16))
          (g [] (.toString (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16))]
    (UUID. (.toString (.append (goog.string.StringBuffer.)
       (f) (f) (f) (f) (f) (f) (f) (f) "-" (f) (f) (f) (f)
       "-4" (f) (f) (f) "-" (g) (f) (f) (f) "-"
       (f) (f) (f) (f) (f) (f) (f) (f) (f) (f) (f) (f))))))

(defn byte->hex [b]
  (-> b
      (bit-and 0xff)
      (+ 0x100)
      (.toString 16)
      (.substring 1)))


(defn hash->str [bytes]
  (apply str (map byte->hex bytes)))


;; taken from http://jsperf.com/uint8array-vs-array-encode-to-utf8/2
;; which is taken from //http://user1.matsumoto.ne.jp/~goma/js/utf.js
;; verified against: "小鳩ちゃんかわいいなぁ"
(defn utf8
  "Encodes a string as UTF-8 in an unsigned js array."
  [s]
  (into-array
   (mapcat
    (fn [pos]
      (let [c (.charCodeAt s pos)]
        (cond (<= c 0x7F) [(bit-and c 0xFF)]
              (<= c 0x7FF) [(bit-or 0xC0 (bit-shift-right c 6))
                            (bit-or 0x80 (bit-and c 0x3F))]
              (<= c 0xFFFF) [(bit-or 0xE0 (bit-shift-right c 12))
                             (bit-or 0x80 (bit-and (bit-shift-right c 6) 0x3F))
                             (bit-or 0x80 (bit-and c 0x3F))]
              :default (let [j (loop [j 4]
                                 (if (pos? (bit-shift-right c (* j 6)))
                                   (recur (inc j))
                                   j))
                             init (bit-or (bit-and (bit-shift-right 0xFF00 j) 0xFF)
                                          (bit-shift-right c (* 6 (dec j))))]
                         (conj (->> (range (dec j))
                                    reverse
                                    (map #(bit-or 0x80
                                                  (bit-and (bit-shift-right c (* 6 %))
                                                           0x3F))))
                               init)))))
    (range (.-length s)))))

#_(utf8 "小鳩ちゃんかわいいなぁ")

(defn signed-byte [b]
  (if (> b 127) (- b 256) b))

(defn uuid5
  "Generates a uuid5 from a sha-1 hash byte sequence.
Our hash version is coded in first 2 bits."
  [sha-hash]
  (let [[hb1 hb2 hb3 hb4 hb5 hb6 hb7 hb8
         lb1 lb2 lb3 lb4 lb5 lb6 lb7 lb8] sha-hash]
    (-> [(bit-clear (bit-clear hb1 7) 6) hb2 hb3 hb4 hb5 hb6 (bit-or 0x50 (bit-and 0x5f hb7)) hb8
         (bit-clear (bit-set lb1 7) 6) lb2 lb3 lb4 lb5 lb6 lb7 lb8]
        hash->str
        ((fn [s] (str (apply str (take 8 s))
                     "-" (apply str (take 4 (drop 8 s)))
                     "-" (apply str (take 4 (drop 12 s)))
                     "-" (apply str (take 4 (drop 16 s)))
                     "-" (apply str (drop 20 s)))))
        UUID.)))

(defn sha512-message-digest []
  (goog.crypt.Sha512.))

(defn- digest
  [md bytes-or-seq-of-bytes]
  (.reset md)
  (if (seq? bytes-or-seq-of-bytes)
    (doseq [bs bytes-or-seq-of-bytes]
      (.update md bs))
    (.update md bytes-or-seq-of-bytes))
  (.digest md))

(defn coerce-seq [resetable-md md-create-fn seq]
  (.reset resetable-md)
  (loop [s seq]
    (let [f (first s)]
      (when f
        (.update resetable-md (-coerce f resetable-md md-create-fn))
        (recur (rest s)))))
  (.digest resetable-md))

(comment
  (.log js/console (coerce-seq (sha512-message-digest) sha512-message-digest '(1 2 3)))

  (.log js/console (into-array (map signed-byte ))))

(defn padded-coerce
  "Commutatively coerces elements of collection, seq entries should be crypto hashed
  to avoid collisions in XOR."
  [seq resetable-md md-create-fn]
  (let [len (min (long (alength (first seq))) max-entropy)]
    (reduce (fn padded-xor [acc elem]
              (loop [i 0]
                (when (< i len)
                  (aset acc i (byte (bit-xor (aget acc i) (aget elem i))))
                  (recur (inc i))))
              acc)
            (into-array (repeat len 0))
            seq)))

(defn encode [magic a]
  (.concat #js [magic] a))

(defn encode-safe [resetable-md a]
  (if (< (count a) split-size)
    (let [len (alength a)
          ea (into-array (repeat len 0))]
      (loop [i 0]
        (when-not (= i len)
          (let [e (aget a i)]
            (when (and (> e (byte 0))
                       (< e (byte 30)))
              (aset ea i (byte 1))))
          (recur (inc i))))
      (.concat a ea))
    (digest resetable-md a)))

(defn- str->utf8 [x]
  (-> x str utf8))

(extend-protocol IHashCoercion
  nil
  (-coerce [this resetable-md md-create-fn]
    (encode (:nil magics) #js[]))

  boolean
  (-coerce [this resetable-md md-create-fn]
    (encode (:boolean magics) #js [(if this 41 40)]))

  string
  (-coerce [this resetable-md md-create-fn]
    (encode (:string magics) (encode-safe resetable-md (str->utf8 this))))

  number
  (-coerce [this resetable-md md-create-fn]
    (encode (:number magics) (str->utf8 this)))

  js/Date
  (-coerce [this resetable-md md-create-fn]
    ;; utf8 is not needed, can be optimized
    (encode (:inst magics) (str->utf8 (.getTime this))))

  cljs.core/UUID
  (-coerce [this resetable-md md-create-fn]
    (encode (:uuid magics) (str->utf8 (.-uuid this))))

  cljs.core/Symbol
  (-coerce [this resetable-md md-create-fn]
    (encode (:symbol magics) (encode-safe resetable-md (str->utf8 this))))

  cljs.core/Keyword
  (-coerce [this resetable-md md-create-fn]
    (encode (:keyword magics) (encode-safe resetable-md (str->utf8 this))))

  default
  (-coerce [this resetable-md md-create-fn]
    (cond (satisfies? IRecord this)
          (let [[tag val] (as-value this)]
            (encode (:literal magics)
                    (coerce-seq resetable-md
                                md-create-fn
                                [(-coerce tag resetable-md md-create-fn)
                                 (-coerce val resetable-md md-create-fn)])))

          (satisfies? ISeq this)
          (encode (:seq magics) (coerce-seq resetable-md md-create-fn this))

          (satisfies? IVector this)
          (encode (:vector magics) (coerce-seq resetable-md md-create-fn this))

          (satisfies? IMap this)
          (encode (:map magics)
                  (digest resetable-md
                          (padded-coerce (map #(-coerce %
                                                        resetable-md
                                                        md-create-fn)
                                              (seq this))
                                         resetable-md
                                         md-create-fn)))

          (satisfies? ISet this)
          (encode (:set magics)
                  (digest resetable-md
                          (padded-coerce (map #(digest resetable-md
                                                       (-coerce % resetable-md md-create-fn))
                                              (seq this))
                                         resetable-md
                                         md-create-fn)))

          ;; TODO
          #_(instance? js/UInt8Array this)
          #_(hash-fn (conj (mapcat benc this)
                           (:binary magics)))

          :else
          (let [[tag val] (as-value this)]
            (encode (:literal magics)
                    (coerce-seq resetable-md
                                md-create-fn
                                [(-coerce tag resetable-md md-create-fn)
                                 (-coerce val resetable-md md-create-fn)]))))))


(comment

  (def datom-vector (doall (vec (repeat 10000 {:db/id 18239
                                               :person/name "Frederic"
                                               :person/familyname "Johanson"
                                               :person/street "Fifty-First Street 53"
                                               :person/postal 38237
                                               :person/telefon "02343248474"
                                               :person/weeight 0.3823}))))

  (time (uuid datom-vector))
  )
