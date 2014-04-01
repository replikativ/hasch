(ns hasch.platform
  (:require [goog.crypt.Sha1]
            [hasch.benc :refer [IHashCoercion -coerce magics padded-coerce]]))


(defn sha-1 [bytes]
  (let [md (goog.crypt.Sha1.)
        sarr (into-array (map #(if (neg? %) (+ % 256) %) bytes))]
    (.update md sarr)
    (map #(if (> % 127)
            (- % 256)
            %) (.digest md))))


(defn byte->hex [b]
  (-> b
      (bit-and 0xff)
      (+ 0x100)
      (.toString 16)
      (.substring 1)))

(defn hash->str [bytes]
  (apply str (map byte->hex bytes)))


(defn benc [n]
  (if (< n -98)
    (list -98 (+ n 98))
    (list n)))


(defn encode [input]
  (mapcat benc
    (map
      #(bit-and (.charCodeAt input %) 0xFF)
      (range (.-length input)))))

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


(extend-protocol IHashCoercion
  js/Boolean
  boolean
  (-coerce [this hash-fn] (list (:boolean magics) (if this 1 0)))

  js/String
  (-coerce [this hash-fn] (conj (encode this) (:string magics)))

  js/Number
  (-coerce [this hash-fn] (conj (encode (str this)) (:number magics)))

  js/Date
  (-coerce [this hash-fn] (conj (encode (str (.getTime this))) (:inst magics)))

  cljs.core/UUID
  (-coerce [this hash-fn] (conj (encode (.-uuid this)) (:uuid magics)))

  nil
  (-coerce [this hash-fn] (list (:nil magics)))

  cljs.core/Symbol
  (-coerce [this hash-fn] (conj (mapcat benc
                                        (concat (encode (or (namespace this) ""))
                                                (encode (name this))))
                                (:symbol magics)))

  cljs.core/Keyword
  (-coerce [this hash-fn] (conj (mapcat benc
                                        (concat (encode (or (namespace this) ""))
                                                (encode (name this))))
                                (:keyword magics)))

  cljs.core/EmptyList
  cljs.core/List
  cljs.core/Cons
  (-coerce [this hash-fn] (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                                         (:seq magics))))

  cljs.core/PersistentVector
  (-coerce [this hash-fn] (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                                         (:vector magics))))

  cljs.core/PersistentArrayMap
  (-coerce [this hash-fn] (hash-fn (conj (padded-coerce this hash-fn)
                                         (:map magics))))

  cljs.core/PersistentHashSet
  (-coerce [this hash-fn] (hash-fn (conj (padded-coerce this hash-fn)
                                         (:set magics)))))




(defn boolean? [val]
  (= (type val) js/Boolean))


(defn uuid? [val]
  (= (type val) cljs.core.UUID))


(defn date? [val]
  (= (type val) js/Date))
