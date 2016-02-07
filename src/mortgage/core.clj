(ns mortgage.core
  (:require [schema.core :as s]))

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
; cmd-shift-l sends file to repl (equivalent of cpr in fireplace)
; cmd-shift-p sends current form to repl (equivalent of cpp in fireplace)
; cmd-alt-e: view repl history
;
; documentation
; cmd-p: show parameters this function takes
; ctrl-j: show docstring
; cmd-alt-l: reformat code
;
; navigation
; cmd-f7: show usages
; cmd-e: recent files - cursive docs say you can use this instead of tabs, we'll see about that
; cmd-shift-o: search for file
; cmd-f12: display list of symbols defined in current file, begin typing to search
; cmd-b: jump to declaration (siiiiiick - works for builtin clojure functions too)
; f3: toggle anonymous bookmark
; alt-f3: add bookmark with mnemonic (use numbers)
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

; per https://smartasset.com/taxes/oregon-property-tax-calculator#7KnveUIptd
(def multnomah-county-property-tax-rate 0.01123)

; per http://www.valuepenguin.com/average-cost-of-homeowners-insurance
(def average-monthly-homeowners-insurance-payment 47.98)

(s/defschema Mortgage
  {:house-price             s/Int
   :apr                     s/Num
   :down-payment-percentage s/Num
   :num-years               s/Int})

(s/defn make-mortgage :- Mortgage
  [house-price apr down-payment-percentage num-years]
  {:house-price             house-price
   :apr                     apr
   :down-payment-percentage down-payment-percentage
   :num-years               num-years})

(s/defn get-down-payment :- s/Num
  [m :- Mortgage]
  (* (:house-price m)
     (:down-payment-percentage m)))

(s/defn get-loan-amount :- s/Num
  [m :- Mortgage]
  (- (:house-price m)
     (get-down-payment m)))

(s/defn is-jumbo-loan :- s/Bool
  [m :- Mortgage]
  (> (get-loan-amount m)
     417000))

(s/defn total-mortgage-price :- s/Num
  [m :- Mortgage]
  (let [r (/ (:apr m) 12)
        n (* (:num-years m) 12)]
    (* (/ (* (get-loan-amount m)
             r)
          (- 1
             (Math/pow (+ 1 r)
                       (- n))))
       n)))

(s/defn base-monthly-payment :- s/Num
  [m :- Mortgage]
  (/ (total-mortgage-price m)
     (* (:num-years m)
        12)))

;(s/defn total-monthly-payment)

(def foo (make-mortgage 500000 0.0325 0.2 30))

(comment

  (base-monthly-payment foo)
  (total-mortgage-price foo)

  )
