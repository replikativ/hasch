(ns hasch.base64
  (:require #?(:clj [clojure.data.codec.base64 :as b64]
               :cljs [goog.crypt.base64])
            #?(:cljs [cljs.reader :as r])))

(defn encode
  "Returns a base64 encoded String."
  [byte-arr]
  #?(:clj (String. ^bytes (b64/encode byte-arr) "UTF-8")
     :cljs (goog.crypt.base64.encodeByteArray byte-arr)))

(defn decode
  "Returns a byte-array for encoded String."
  [^String base64]
  #?(:clj (b64/decode (.getBytes base64 "UTF-8"))
     :cljs (goog.crypt.base64.decodeStringToByteArray base64)))
