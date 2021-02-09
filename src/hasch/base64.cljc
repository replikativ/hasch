(ns hasch.base64
  #?(:cljs (:require [goog.crypt.base64]
                     [cljs.reader :as r]))
  #?(:clj (:import (java.util Base64))))

(defn encode
  "Returns a base64 encoded String."
  [byte-arr]
  #?(:clj (String. (.encode (Base64/getEncoder)
                            ^bytes byte-arr)
                   "UTF-8")
     :cljs (goog.crypt.base64.encodeByteArray byte-arr)))

(defn decode
  "Returns a byte-array for encoded String."
  [^String base64]
  #?(:clj (.decode (Base64/getDecoder) base64)
     :cljs (goog.crypt.base64.decodeStringToByteArray base64)))
