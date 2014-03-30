(ns hasch.platform
  (:require [goog.crypt.Sha1]
            [hasch.benc :refer [IHashCoercion -coerce magics]]))


(defn sha-1 [bytes]
  (let [md (goog.crypt.Sha1.)
        sarr (apply js/Array bytes)]
    (.update md sarr)
    (.digest md)))


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
  (-coerce [this hash-fn] (list (:boolean magics) (if this 1 0)))

  js/String
  (-coerce [this hash-fn] (conj (encode this) (:string magics)))

  js/Number
  (-coerce [this hash-fn] (conj (encode (str this)) (:number magics)))

  js/Date
  (-coerce [this hash-fn] (conj (encode (str this)) (:inst magics))))
