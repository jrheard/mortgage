(ns mortgage.core-test
  (:require [clojure.test :refer :all]
            [mortgage.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(deftest blat
  (is (= 1 1))

  (let [foo (fn [a] (+ a 10))]
    (is (= (foo 1)
           11))))
