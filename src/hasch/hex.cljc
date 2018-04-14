(ns hasch.hex
  (:require [hasch.platform :refer [byte->hex]]))

(defn encode [raw]
  (apply str (map byte->hex raw)))

(comment
  (encode (byte-array (range 100))))


