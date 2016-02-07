(ns mortgage.core-test
  (:require [clojure.test :refer :all]
            [mortgage.core :refer :all]
            [schema.core :as s]
            [schema.test]))

(use-fixtures :once schema.test/validate-schemas)

(deftest making-mortgage
  (testing "happy path"
    (is (= (make-mortgage 500000 0.035 0.15)
           {:house-price             500000
            :apr                     0.035
            :down-payment-percentage 0.15})))

  (testing "garbage input"
    (is (thrown? Exception
                 (make-mortgage "foo" "bar" "baz")))))
