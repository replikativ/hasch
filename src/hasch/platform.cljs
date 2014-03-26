(ns hasch.platform)

(defprotocol IHashCoercion
  (-coerce [this hash-fn]))

;; changes break hashes!
(def magics {:string -99
             :symbol -100
             :keyword -101
             :number -102
             :vector -103
             :seq -104
             :map -105
             :set -106
             :nil -107
             :boolean -108
             :character -109
             :uuid -110
             :inst -111})


(defn sha-1 [])

(defn benc
  "Dumb binary encoding which ensures basic types (incl. collections)
cannot collide with different content. MAY NOT CHANGE OR BREAKS HASHES."
  [x]
  (let [b (byte x)]
    (if (< b -98)
      (list -98 (+ b 98))
      (list b))))


(defn to-byte [n]
  (if (< n -98)
    (list -98 (+ n 98))
    (list n)))


(defn get-js-string [input]
  (mapcat to-byte
    (map
      #(bit-and (.charCodeAt input %) 0xFF)
      (range (.-length input)))))


(extend-protocol IHashCoercion
  js/Boolean
  (-coerce [this hash-fn] (list (:boolean magics) (if this 1 0)))

  js/String
  (-coerce [this hash-fn] (conj (get-js-string this) (:string magics)))

  js/Number
  (-coerce [this hash-fn] (conj (get-js-string (str this)) (:number magics)))

  js/Date
  (-coerce [this hash-fn] (conj (get-js-string (str this)) (:inst magics))))


;; --- some testing, open console in /resources/public/index.html for output ---

(.log js/console (str (-coerce true sha-1)))
(.log js/console (str (-coerce "abc" sha-1)))
(.log js/console (str (-coerce 123 sha-1)))
(.log js/console (str (-coerce 1.23 sha-1)))
(.log js/console (str (-coerce (js/Date.) sha-1)))
