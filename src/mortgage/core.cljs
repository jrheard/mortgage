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

;;; UI

(sm/defschema UIState {:mortgages         [Mortgage]
                       :selected-mortgage Mortgage
                       :ui-event-chan     s/Any})

(sm/defschema DataPoint {:mortgage Mortgage
                         :value    s/Num})

(sm/defn format-number [n :- s/Num]
  (str "$" (.toLocaleString n)))

(sm/defn draw-bar-graph [y-axis-label :- s/Str
                         data-points :- [DataPoint]
                         state]
  [:svg {:width 500 :height 300}
   [:rect {:x              0
           :y              0
           :width          500
           :height         300
           :fill           "#FFF"
           :on-mouse-enter #(put! (:ui-event-chan state) {:type :selection-end})}]
   [:line {:x1           100 :y1 20
           :x2           100 :y2 280
           :stroke       "black"
           :stroke-width 1}]
   [:line {:x1           100 :y1 280
           :x2           500 :y2 280
           :stroke       "black"
           :stroke-width 1}]

   [:text {:x 70 :y 235 :transform "rotate(270 75 230)"} y-axis-label]
   [:text {:x 90 :y 30 :text-anchor "end"} (format-number (int (apply max (map :value data-points))))]

   (for [[index point] (map-indexed vector data-points)]
     (let [x (+ 110 (* index 50))
           height (* 250
                     (/ (:value point) (apply max (map :value data-points))))]
       ^{:key (str "rect-" point)} [:rect.bar {:x              (- x 50)
                                               :y              280
                                               :width          40
                                               :transform      (str "rotate(180 " x " " 280 ")")
                                               :height         height
                                               :class          (when (= (:selected-mortgage state) (:mortgage point))
                                                                 "selected")
                                               :on-mouse-enter #(put! (:ui-event-chan state) {:type     :selection-start
                                                                                              :mortgage (:mortgage point)})}]))

   (when (:selected-mortgage state)
     (let [point (first (filter #(= (:mortgage %)
                                    (:selected-mortgage state))
                                data-points))
           index (.indexOf (to-array data-points) point)
           x (+ 110 (* index 50))]
       [:text {:x         (+ x 10)
               :y         320
               :class     "selected"
               :transform (str "rotate(270 " x " " 280 ")")}
        (.toLocaleString (int (:value point)))]))])

(sm/defn draw-money-wasted [mortgages :- [Mortgage]
                            state :- UIState]
  [draw-bar-graph
   "Money Wasted On Interest"
   (for [m mortgages]
     {:mortgage m
      :value    (-> m total-price-breakdown :interest)})
   state])

(sm/defn draw-monthly-payment [mortgages :- [Mortgage]
                               state :- UIState]
  [draw-bar-graph
   "Total Monthly Payment"
   (for [m mortgages]
     {:mortgage m
      :value    (full-monthly-payment-amount m)})
   state])

(sm/defn draw-mortgage [m :- Mortgage
                        state :- UIState]
  [:tr.mortgage
   {:on-mouse-enter #(put! (:ui-event-chan state) {:type     :selection-start
                                                   :mortgage m})
    :on-mouse-leave #(put! (:ui-event-chan state) {:type :selection-end})
    :class          (when (= m (:selected-mortgage state))
                      "selected")}
   [:td (:house-price m)]
   [:td (:apr m)]
   [:td (:down-payment-percentage m)]
   [:td (:num-years m)]])

(defn draw-state [state]
  (let [state @state]
    [:div.content
     (let [mortgages (filter #(= (:num-years %) 30)
                             (:mortgages state))]
       [:div.graphs
        [draw-money-wasted mortgages state]
        [draw-monthly-payment mortgages state]])
     (let [mortgages (filter #(= (:num-years %) 15)
                             (:mortgages state))]
       [:div.graphs
        [draw-money-wasted mortgages state]
        [draw-monthly-payment mortgages state]])
    [:table
     [:tbody
      [:tr
       [:th "House Price"]
       [:th "APR"]
       [:th "% Down"]
       [:th "Duration"]]
      (for [[index m] (map-indexed vector (:mortgages state))]
        ^{:key (str "mortgage-" index)} [draw-mortgage m state])]] ] ) )

(def some-mortgages
  [(make-mortgage 550000 0.0325 0.2 30)
   (make-mortgage 500000 0.0325 0.2 30)
   (make-mortgage 450000 0.0375 0.2 30)
   (make-mortgage 400000 0.0375 0.2 30)
   (make-mortgage 550000 0.0325 0.2 15)
   (make-mortgage 500000 0.0325 0.2 15)
   (make-mortgage 450000 0.0375 0.2 15)
   (make-mortgage 400000 0.0375 0.2 15)])

(defonce state (r/atom {:mortgages         some-mortgages
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
