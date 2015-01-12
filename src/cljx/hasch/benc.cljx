(ns ^:shared hasch.benc
  "Binary encoding of EDN values.")

(set! *warn-on-reflection* true)

(defprotocol IHashCoercion
  (-coerce [this resetable-md md-create-fn]))

;; changes break hashes!
(def magics {:nil (byte 0)
             :boolean (byte 1)
             :number (byte 2)
             :string (byte 3)
             :symbol (byte 4)
             :keyword (byte 5)
             :inst (byte 6)
             :uuid (byte 7)
             :seq (byte 8)
             :vector (byte 9)
             :map (byte 10)
             :set (byte 11)
             :literal (byte 12)
             :binary (byte 13)})


#_(defn padded-coerce
  "Commutatively coerces elements of collection, padding ensures all bits
are included in the hash."
  [coll resetable-md md-create-fn encode]
  (reduce (fn padded-xor [^bytes acc ^bytes elem]
            (let [[^bytes a ^bytes b] (if (> (alength acc) (alength elem)) [acc elem] [elem acc])]
              (loop [i 0]
                (when (< i (alength b))
                  (aset a i (byte (bit-xor (aget a i) (aget b i))))
                  (recur (inc i))))
              a))
          (byte-array 0)
          (map #(encode (:vector magics) (-coerce % resetable-md md-create-fn)) (seq coll))))
