(ns ^:shared hasch.core
  "Hashing functions for EDN."
  (:require [hasch.benc :refer [IHashCoercion -coerce benc magics padded-coerce]]
            [hasch.platform :refer [boolean? date?]]))

(def uuid4 hasch.platform/uuid4)
(def uuid5 hasch.platform/uuid5)
(def uuid? hasch.platform/uuid?)
(def hash->str hasch.platform/hash->str)
(def sha-1 hasch.platform/sha-1)


(defn- atomic? [val]
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
  "Hash an edn value with SHA-1 by default or a compatible hash function of choice."
  ([val] (edn-hash val sha-1))
  ([val hash-fn]
     (let [coercion (map byte (-coerce val hash-fn))]
       (if (atomic? val)
         (hash-fn coercion)
         coercion))))

(defn uuid
  "Creates random UUID-4 without argument or UUID-5 for the argument value."
  ([] (uuid4))
  ([val] (-> val edn-hash uuid5)))
