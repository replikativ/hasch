(ns hasch.core
  "Hashing functions for EDN."
  (:require [hasch.benc :refer [IHashCoercion -coerce]]
            [hasch.platform :as platform]))

(def uuid4 platform/uuid4)
(def uuid5 platform/uuid5)
(def hash->str platform/hash->str)

(defn edn-hash
  "Hash an edn value with SHA-512 by default or a compatible hash function of choice."
  ([val] (edn-hash val hasch.platform/sha512-message-digest))
  ([val md-create-fn]
   (let [md (md-create-fn)]
     (let [res (-coerce val md md-create-fn)]
       (.reset md)
       (.update md res)
       (into-array (map platform/signed-byte (.digest md)))))))

(defn uuid
  "Creates random UUID-4 without argument or UUID-5 for the argument value."
  ([] (uuid4))
  ([val] (-> val edn-hash uuid5)))
