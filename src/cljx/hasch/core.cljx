(ns ^:shared hasch.core
  "Hashing functions for EDN."
  (:require [hasch.benc :refer [IHashCoercion -coerce benc magics padded-coerce]]
            [hasch.platform :refer [sha-1 boolean? uuid? date? byte->hex]]))


(def uuid5 hasch.platform/uuid5)

(defn atomic? [val]
  (or (nil? val)
      (boolean? val)
      #+clj (char? val)
      (symbol? val)
      (keyword? val)
      (string? val)
      (number? val)
      (uuid? val)
      (date? val)))


(defn edn-hash
  ([val] (edn-hash val sha-1))
  ([val hash-fn]
     (let [coercion (map byte (-coerce val hash-fn))]
       (if (atomic? val)
         (hash-fn coercion)
         coercion))))
