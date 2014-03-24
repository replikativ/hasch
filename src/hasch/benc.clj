(ns ^:shared hasch.benc
  "Binary encoding of EDN values.")

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


(defn benc
  "Dumb binary encoding which ensures basic types (incl. collections)
cannot collide with different content. MAY NOT CHANGE OR BREAKS HASHES."
  [x]
  (let [b (byte x)]
    (if (< b -98)
      (list -98 (+ b 98))
      (list b))))


(defn padded-coerce
  "Commutatively coerces elements of collection, padding ensures all bits
are included in the hash."
  [coll hash-fn]
  (reduce (fn padded-xor [acc elem]
            (let [[a b] (if (> (count acc)
                               (count elem))
                          [acc (concat elem (repeat (- (count acc)
                                                       (count elem))
                                                    0))]
                          [(concat acc (repeat (- (count elem)
                                                  (count acc))
                                               0)) elem])]
              (map bit-xor a b)))
          '()
          (map #(-coerce % hash-fn) (seq coll))))
