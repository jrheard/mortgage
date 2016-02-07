(ns mortgage.core-test
  (:require [clojure.test :refer :all]
            [mortgage.core :refer :all]
            [schema.core :as s]
            [schema.experimental.complete :as c]
            [schema.experimental.generators :as g]
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

(deftest maths
  (let [a-mortgage (make-mortgage 500000 0.035 0.15)]
    (testing "get-loan-amount"
      (is (== (get-loan-amount a-mortgage)
              425000)))

    (testing "get-down-payment"
      (is (== (get-down-payment a-mortgage)
              75000)))))

; TODO - structural editing and the vim dd command do not play well together.
; dd kills all closing parens on the line, it doesn't preserve some of them like it should.
; see if this is a known issue, report if not

(deftest jumbo
  (testing "is-jumbo"
    (is (= (is-jumbo-loan (c/complete {:house-price 400000 :down-payment-percentage 0} Mortgage))
           false))

    (is (= (is-jumbo-loan (c/complete {:house-price 500000 :down-payment-percentage 0.05} Mortgage))
           true))))

(comment
  (c/complete {:house-price 100000} Mortgage)
  (g/generate Mortgage)
  )
