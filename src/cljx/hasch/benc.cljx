(ns ^:shared hasch.benc
  "Binary encoding of EDN values.")

(defprotocol IHashCoercion
  (-coerce [this hash-fn]))

;; changes break hashes!
(def magics {:nil -99
             :boolean -100
             :number -101
             :string -102
             :symbol -103
             :keyword -104
             :inst -105
             :uuid -106
             :seq -107
             :vector -108
             :map -109
             :set -110})


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
