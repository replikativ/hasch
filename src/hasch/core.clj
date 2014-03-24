(ns ^:shared hasch.core
  "Hashing functions for EDN."
  (:require [hasch.benc :refer [IHashCoercion -coerce benc magics padded-coerce]]
            [hasch.platform :refer :all]))


(extend-protocol IHashCoercion
  nil
  (-coerce [this hash-fn] (list (:nil magics)))

  clojure.lang.Symbol
  (-coerce [this hash-fn] (conj (mapcat benc
                                        (concat (namespace this)
                                                (name this)))
                                (:symbol magics)))

  clojure.lang.Keyword
  (-coerce [this hash-fn] (conj (mapcat benc
                                        (concat (namespace this)
                                                (name this)))
                                (:keyword magics)))

  clojure.lang.ISeq
  (-coerce [this hash-fn] (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                                         (:seq magics))))

  clojure.lang.IPersistentVector
  (-coerce [this hash-fn] (hash-fn (conj (mapcat #(-coerce % hash-fn) this)
                                         (:vector magics))))

  clojure.lang.IPersistentMap
  (-coerce [this hash-fn] (hash-fn (conj (padded-coerce this hash-fn)
                                         (:map magics))))

  clojure.lang.IPersistentSet
  (-coerce [this hash-fn] (hash-fn (conj (padded-coerce this hash-fn)
                                         (:set magics))))

  ;; implement tagged literal instead
  clojure.lang.IRecord
  (-coerce [this hash-fn] (conj (concat (mapcat benc (str (type this)))
                                        (padded-coerce this hash-fn))
                                (:record magics))))

(defn atom? [val]
  (or (nil? val)
      (boolean? val)
      (char? val)
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
       (if (atom? val)
         (hash-fn coercion)
         coercion))))


(defn hash->str [bytes]
  (apply str (map byte->hex bytes)))
