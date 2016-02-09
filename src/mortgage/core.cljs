(ns mortgage.core
  (:require [schema.core :as s])
  (:require-macros [schema.core :as sm]))

; paredit
; cmd+shift+j, cmd+shift+k - move right paren back/forth
; cmd+ctrl+j, cmd+ctrl+k - move left paren back/forth
; cmd-shift-9 wraps with ()
; ctrl-S splices: removes a ()
; cmd-shift-up and cmd-shift-down move a form forward and backward in its containing list, neat
; alt-shift-s is split
; cmd-ctrl-s is join (?! i hate this pair of keybinds)
;
; repl commands
; cmd-shift-r: start repl
; cmd-shift-l sends file to repl (equivalent of cpr in fireplace)
; cmd-shift-p sends current form to repl (equivalent of cpp in fireplace)
; cmd-alt-e: view repl history

; documentation
; cmd-p: show parameters this function takes
; ctrl-j: show docstring
;
; assorted
; cmd-alt-l: reformat code
; shift-f6: rename
;
; navigation
; cmd-f7: show usages
; cmd-e: recent files - cursive docs say you can use this instead of tabs, we'll see about that
; cmd-shift-o: search for file
; cmd-f12: display list of symbols defined in current file, begin typing to search
; cmd-b: jump to declaration (siiiiiick - works for builtin clojure functions too)
; alt-f3: toggle anonymous bookmark
; f3: add bookmark with mnemonic (use numbers)
; ctrl-<number>: jump to numbered mnemonic bookmark
; cmd-f3: view all bookmarks
; cmd-up: open navigation bar, interact w/ it with arrow keys; consider using instead of project browser
;
; testing
; cmd-t: run tests in current ns
; cmd-shift-t: run test under caret
;
; debugging
; alt-f8: toggle breakpoint
; ctrl-d: run program in debug mode
; f8: step over
; f7: step into
; shif-f8:  step out
;
; one last vim command to memorize:
; ctrl-w c - closes window (for use with :split, etc)

; per https://smartasset.com/taxes/oregon-property-tax-calculator#7KnveUIptd
(def multnomah-county-property-tax-rate 0.01123)

; per http://www.valuepenguin.com/average-cost-of-homeowners-insurance
(def average-monthly-homeowners-insurance-payment 47.98)

; TODO - current situation is that the cursive repl doesn't automatically switch to mortgage.core ns when cmd-shift-l happens

(sm/defschema Mortgage
  {:house-price             s/Int
   :apr                     s/Num
   :down-payment-percentage s/Num
   :num-years               s/Int})


(sm/defschema MonthlyPayment
  {:principal    s/Num
   :interest     s/Num
   :property-tax s/Num
   :insurance    s/Num})

(sm/defn make-mortgage :- Mortgage
  [house-price apr down-payment-percentage num-years]
  {:house-price             house-price
   :apr                     apr
   :down-payment-percentage down-payment-percentage
   :num-years               num-years})

(sm/defn get-down-payment :- s/Num
  [m :- Mortgage]
  (* (:house-price m)
     (:down-payment-percentage m)))

(sm/defn get-loan-amount :- s/Num
  [m :- Mortgage]
  (- (:house-price m)
     (get-down-payment m)))

(sm/defn is-jumbo-loan :- s/Bool
  [m :- Mortgage]
  (> (get-loan-amount m)
     417000))

; per http://www.calcunation.com/calculator/mortgage-total-cost.php
(sm/defn total-mortgage-price :- s/Num
  [m :- Mortgage]
  (let [monthly-interest-rate (/ (:apr m) 12)
        num-months (* (:num-years m) 12)]
    (* (/ (* (get-loan-amount m)
             monthly-interest-rate)
          (- 1
             (Math/pow (+ 1 monthly-interest-rate)
                       (- num-months))))
       num-months)))

(sm/defn base-monthly-payment-amount :- s/Num
  [m :- Mortgage]
  (/ (total-mortgage-price m)
     (* (:num-years m)
        12)))

(sm/defn get-payments :- [MonthlyPayment]
  [m :- Mortgage]
  (loop [principal (get-loan-amount m)
         result []]
    (if (> principal 0)
      (let [interest (/ (* principal (:apr m))
                        12)
            payment {:principal    (- (base-monthly-payment-amount m) interest)
                     :interest     interest
                     :property-tax (/ (* multnomah-county-property-tax-rate
                                         (:house-price m))
                                      12)
                     :insurance    average-monthly-homeowners-insurance-payment}]
        (recur (- principal (:principal payment))
               (conj result payment)))
      result)))

(sm/defn full-monthly-payment-amount :- s/Num
  [m :- Mortgage]
  (->> m
       get-payments
       first
       vals
       (apply +)))

(sm/defn total-price-breakdown :- {:principal       s/Num
                                   :interest        s/Num
                                   :monthly-payment s/Num}
  [m :- Mortgage]
  (let [payments (get-payments m)]
    {:principal       (apply + (map :principal payments))
     :interest        (apply + (map :interest payments))
     :total           (total-mortgage-price m)
     :monthly-payment (full-monthly-payment-amount m)}))

(def some-mortgages
  [(make-mortgage 550000 0.0325 0.2 30)
   (make-mortgage 500000 0.0325 0.2 30)
   (make-mortgage 500000 0.0325 0.2 15)
   (make-mortgage 450000 0.0375 0.2 30)
   (make-mortgage 450000 0.0375 0.2 15)
   (make-mortgage 400000 0.0375 0.2 30)
   (make-mortgage 400000 0.0375 0.2 15)])

(def foo (first some-mortgages))

; TODO - cljs+reagent interface that lets you tweak this? with, like, sliders?

(comment
  (last
    (get-payments foo))

  (total-mortgage-price foo)
  (total-price-breakdown foo)

  (defn foo [bar]
    (+ bar 3))

  (map total-price-breakdown some-mortgages)
  (map total-mortgage-price some-mortgages)

  (map total-price-breakdown some-mortgages)

  (apply min-key :total
         (map total-price-breakdown some-mortgages))
  )
