(ns mortgage.core
  (:require [reagent.core :as r]
            [schema.core :as s])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [schema.core :as sm])
  (:use [cljs.core.async :only [chan <! >! put!]]))

;;;;;;;;;
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

(sm/defn -get-payments :- [MonthlyPayment]
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

(def get-payments (memoize -get-payments))

(sm/defn full-monthly-payment-amount :- s/Num
  [m :- Mortgage]
  (->> m
       get-payments
       first
       vals
       (apply +)))

;;;;;;;;
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
                   is-selected-mortgage :- s/Bool
                   ui-event-chan]
  (js/console.log "drawing bar")
  (let [x (+ 110 (* offset 30))
        height (* 250
                  (/ (:value point) max-value))]
    [:rect.bar {:x              (- x 20)
                :y              280
                :width          30
                :transform      (str "rotate(180 " x " " 280 ")")
                :height         height
                :class          (when is-selected-mortgage "selected")
                :on-mouse-enter #(put! ui-event-chan {:type     :selection-start
                                                      :mortgage (:mortgage point)})}]))

(sm/defn draw-bar-graph [y-axis-label :- s/Str
                         data-points :- [DataPoint]
                         selected-mortgage :- Mortgage
                         ui-event-chan]
  [:svg {:width 520 :height 300}
   ; Axes + labels
   [:rect {:x              0
           :y              0
           :width          500
           :height         300
           :fill           "#FFF"
           :on-mouse-enter #(put! ui-event-chan {:type :selection-end})}]
   [:line {:x1           100 :y1 20
           :x2           100 :y2 280
           :stroke       "black"
           :stroke-width 1}]
   [:line {:x1           100 :y1 280
           :x2           700 :y2 280
           :stroke       "black"
           :stroke-width 1}]

   [:text {:x 70 :y 235 :transform "rotate(270 75 230)"} y-axis-label]
   [:text {:x 90 :y 30 :text-anchor "end"} (format-number (int (apply max (map :value data-points))))]

   ; Bars
   (for [[index point] (map-indexed vector data-points)]
     ^{:key (str "rect-" index (point :value))} [draw-bar
                                                 point
                                                 index
                                                 (apply max (map :value data-points))
                                                 (= (point :mortgage) selected-mortgage)
                                                 ui-event-chan])

   (when selected-mortgage
     (let [point (first (filter #(= (:mortgage %) selected-mortgage)
                                data-points))
           index (.indexOf (to-array data-points) point)
           x (+ 110 (* index 30))]
       (when point
         [:text {:x         (+ x 10)
                 :y         294
                 :class     "selected"
                 :transform (str "rotate(270 " x " " 280 ")")}
          (.toLocaleString (int (:value point)))])))])

(sm/defn draw-money-wasted [mortgages :- [Mortgage]
                            state :- UIState]
  [draw-bar-graph
   "Money Wasted On Interest"
   (for [m mortgages]
     {:mortgage m
      :value    (->> m
                     get-payments
                     (map :interest)
                     (apply +))})
   (state :selected-mortgage)
   (state :ui-event-chan)])

(sm/defn draw-monthly-payment [mortgages :- [Mortgage]
                               state :- UIState]
  [draw-bar-graph
   "Total Monthly Payment"
   (for [m mortgages]
     {:mortgage m
      :value    (full-monthly-payment-amount m)})
   (state :selected-mortgage)
   (state :ui-event-chan)])

(sm/defn draw-mortgage-table-row [m :- Mortgage
                                  is-selected-mortgage :- s/Bool
                                  ui-event-chan]
  [:tr.mortgage
   {:on-mouse-enter #(put! ui-event-chan {:type     :selection-start
                                          :mortgage m})
    :on-mouse-leave #(put! ui-event-chan {:type :selection-end})
    :class          (when is-selected-mortgage "selected")}
   [:td (:house-price m)]
   [:td (:apr m)]
   [:td (:down-payment-percentage m)]
   [:td (:num-years m)]])

(defn draw-state [state]
  (let [state @state]
    [:div.content
     [:h2 "30-year figures"]
     (let [mortgages (filter #(= (:num-years %) 30)
                             (:mortgages state))]
       [:div.graphs
        [draw-money-wasted mortgages state]
        [draw-monthly-payment mortgages state]])
     [:h2 "15-year figures"]
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
       (when (:selected-mortgage state)
         [draw-mortgage-table-row (:selected-mortgage state) true (:ui-event-chan state)])
       (for [[index m] (map-indexed vector (:mortgages state))]
         ^{:key (str "mortgage-" index)} [draw-mortgage-table-row m (= m (:selected-mortgage state)) (:ui-event-chan state)])]]]))

(def some-mortgages
  (apply concat
         (for [duration [15 30]]
           (for [increment-of-25k (range 14)]
             (make-mortgage (+ 400000
                               (* increment-of-25k 25000))
                            duration)))))

(def state (r/atom {:mortgages         some-mortgages
                    :selected-mortgage nil
                    :ui-event-chan     (chan)}))

(defn handle-ui-events [ui-state]
  (go-loop []
    (let [state @ui-state
          msg (<! (:ui-event-chan state))]
      (case (:type msg)
        :selection-start (swap! ui-state assoc :selected-mortgage (:mortgage msg))
        :selection-end (swap! ui-state assoc :selected-mortgage nil))
      (recur))))

(defn ^:export main []
  (r/render-component [draw-state state]
                      (js/document.getElementById "content"))
  (handle-ui-events state))
