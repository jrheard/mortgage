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
; cmd-y: view source of function
; cmd-alt-l: reformat code
;
; navigation
; cmd-alt-f7: show usages
; alt-f7: find usages
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
; debugging
; alt-f8: toggle breakpoint
; ctrl-d: run program in debug mode
; f8: step over
; f7: step into
; shif-f8:  step out
;
; testing
; cmd-t: run tests in current ns
; cmd-shift-t: run test under caret

(s/defschema Mortgage
  {:house-price             s/Int
   :apr                     s/Num
   :down-payment-percentage s/Num})

(s/defn get-loan-amount :- s/Int
  [mortgage :- Mortgage]
  (- (:house-price mortgage)
     (* (:house-price mortgage)
        (- 1
           (:down-payment-percentage mortgage)))))

(def foo {:house-price             500000
          :apr                     0.035
          :down-payment-percentage 0.15})

(comment
  (get-loan-amount foo))

