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
  ;; TODO
  #_(-> b
      (bit-and 0xff)
      (+ 0x100)
      (Integer/toString 16)
      (.substring 1)))



(defn benc [n]
  (if (< n -98)
    (list -98 (+ n 98))
    (list n)))


(defn encode [input]
  (mapcat benc
    (map
      #(bit-and (.charCodeAt input %) 0xFF)
      (range (.-length input)))))



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
