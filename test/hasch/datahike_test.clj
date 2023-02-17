(ns hasch.datahike-test
  (:require
   [clojure.test :refer :all]
   [datahike.integration-test :as dt]))

(def config {:store {:backend :mem
                     :id "hasch-datahike-test-db"}
             :keep-history? true
             :schema-flexibility :read})

(defn test-fixture [f]
  (dt/integration-test-fixture config))

(use-fixtures :once test-fixture)

(deftest datahike-integration-test
  (is (= :foo :bar))
  (dt/integration-test config))

