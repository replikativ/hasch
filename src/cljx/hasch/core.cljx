(ns hasch.core
  "Hashing functions for EDN."
  (:require [hasch.benc :refer [IHashCoercion -coerce digest]]
            [hasch.platform :as platform]))

(def uuid4 platform/uuid4)
(def uuid5 platform/uuid5)
(def hash->str platform/hash->str)

(defn edn-hash
  "Hash an edn value with SHA-512 by default or a compatible hash function of choice."
  ([val] (edn-hash val hasch.platform/sha512-message-digest))
  ([val md-create-fn]
   (map #(if (neg? %) (+ % 256) %) ;; make unsigned
        (digest (-coerce val md-create-fn) md-create-fn))))

(defn uuid
  "Creates random UUID-4 without argument or UUID-5 for the argument value."
  ([] (uuid4))
  ([val] (-> val edn-hash uuid5)))
