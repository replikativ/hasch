(ns hasch.platform
  (:require [goog.crypt.Sha1]
            [cljs.reader :as reader]
            [clojure.string]
            [hasch.benc :refer [IHashCoercion -coerce magics padded-coerce]]))

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
            (atom (fn [tag val]
                    [(:literal magics) (symbol (pr-str tag)) val]))]
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


(defn sha-1
  "Return a SHA-1 hash in signed byte encoding
for an input sequence in the same encoding."
  [bytes]
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


;; taken from http://jsperf.com/uint8array-vs-array-encode-to-utf8/2
;; which is taken from //http://user1.matsumoto.ne.jp/~goma/js/utf.js
;; verified against: "小鳩ちゃんかわいいなぁ"
(defn utf8
  "Encodes a string as UTF-8 in an unsigned byte value seq."
  [s]
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
   (range (.-length s))))


(defn signed-byte [b]
  (if (> b 127) (- b 256) b))


(defn encode [input]
  (->> input
       utf8
       (map signed-byte)
       (mapcat benc)))

#_(encode "小鳩ちゃんかわいいなぁ")


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
  nil
  (-coerce [this hash-fn] (list (:nil magics)))

  boolean
  (-coerce [this hash-fn] (list (:boolean magics) (if this 1 0)))

  string
  (-coerce [this hash-fn] (conj (encode this) (:string magics)))

  number
  (-coerce [this hash-fn] (conj (encode (str this)) (:number magics)))

  js/Date
  (-coerce [this hash-fn]
    (conj (encode (str (.getTime this))) (:inst magics)))

  cljs.core/UUID
  (-coerce [this hash-fn] (conj (encode (.-uuid this)) (:uuid magics)))

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

  default
  (-coerce [this hash-fn]
    (cond (satisfies? ISeq this)
          (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                         (:seq magics)))

          (satisfies? IVector this)
          (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                         (:vector magics)))

          (satisfies? IMap this)
          (hash-fn (conj (padded-coerce this hash-fn)
                         (:map magics)))

          (satisfies? ISet this)
          (hash-fn (conj (padded-coerce this hash-fn)
                         (:set magics)))

          :else
          (throw "Hashing not supported for:" this))))


(defn boolean? [val]
  (= (type val) js/Boolean))


(defn uuid? [val]
  (= (type val) cljs.core.UUID))


(defn date? [val]
  (= (type val) js/Date))
