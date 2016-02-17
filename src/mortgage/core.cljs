(ns mortgage.core
  (:require [reagent.core :as r]
            [schema.core :as s])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.core :as sm])
  (:use [cljs.core.async :only [chan <! >! put!]]))

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
; testing (unsupported for cljs?)
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

;;; Mortgage math

; per https://smartasset.com/taxes/oregon-property-tax-calculator#7KnveUIptd
(def multnomah-county-property-tax-rate 0.01123)

; per http://www.valuepenguin.com/average-cost-of-homeowners-insurance
(def average-monthly-homeowners-insurance-payment 47.98)

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
  [house-price num-years]
  {:house-price             house-price
   :apr                     (if (> (* house-price 0.8)
                                   417000)
                              0.0325
                              0.0375)
   :down-payment-percentage 0.2
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

;;; UI

(sm/defschema UIState {:mortgages         [Mortgage]
                       :selected-mortgage Mortgage
                       :ui-event-chan     s/Any})

(sm/defschema DataPoint {:mortgage Mortgage
                         :value    s/Num})

(sm/defn format-number [n :- s/Num]
  (str "$" (.toLocaleString n)))

(sm/defn draw-bar [point :- DataPoint
                   offset :- s/Int
                   max-value :- s/Int
                   state :- UIState]
  (js/console.log "drawing bar")
  (let [x (+ 110 (* offset 30))
        height (* 250
                  (/ (:value point) max-value))]
    [:rect.bar {:x              (- x 20)
                :y              280
                :width          30
                :transform      (str "rotate(180 " x " " 280 ")")
                :height         height}]))

(sm/defn draw-bar-graph [y-axis-label :- s/Str
                         data-points :- [DataPoint]
                         state]
  [:svg {:width 520 :height 300}

   (for [[index point] (map-indexed vector data-points)]
     ^{:key (str "rect-" index (point :value))} [draw-bar point index (apply max (map :value data-points)) state])

   ])

(sm/defn draw-monthly-payment [mortgages :- [Mortgage]
                               state :- UIState]
  [draw-bar-graph
   "Total Monthly Payment"
   [{:mortgage (first mortgages)
     :value    (full-monthly-payment-amount (first mortgages))}
    {:mortgage (second mortgages)
     :value    (full-monthly-payment-amount (second mortgages))}
    {:mortgage (nth mortgages 3)
     :value    (full-monthly-payment-amount (nth mortgages 3))}
    ]
   state])

(defn draw-state [state]
  (let [state @state]
    [draw-monthly-payment (:mortgages state) state])
  )

(def some-mortgages
  (apply vector (apply concat
                       (for [duration [15 30]]
                         (for [increment-of-25k (range 14)]
                           (make-mortgage (+ 400000
                                             (* increment-of-25k 25000))
                                          duration))))))

(def state (r/atom {:mortgages         some-mortgages
                    :selected-mortgage nil
                    :ui-event-chan     (chan)}))

(defn handle-ui-events [ui-state]
  (go-loop []
    (let [state @ui-state
          msg (<! (:ui-event-chan state))]
      (condp = (:type msg)
        :selection-start (swap! ui-state assoc :selected-mortgage (:mortgage msg))
        :selection-end (swap! ui-state assoc :selected-mortgage nil))
      (recur))))

(defn ^:export main []
  (r/render-component [draw-state state]
                      (js/document.getElementById "content"))
  (handle-ui-events state))

(comment
  (swap! state update-in [:mortgages 0 :house-price] (fn [x] (rand-int 500000)))
  )
