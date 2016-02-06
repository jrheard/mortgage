(ns mortgage.core)

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

; ok so
; paredit keys to remember / try out:
; cmd+shift+j, cmd+shift+k
; cmd+ctrl+j, cmd+ctrl+k
; cmd-shift-9 wraps with ()
; ctrl-S splices: removes a ()
; cmd-shift-up and cmd-shift-down move a form forward and backward in its containing list, neat
; option-shift-s is split
; cmd-ctrl-s is join (?! i hate this pair of keybinds)
;
; repl commands
; cmd-shift-l sends file to repl (equivalent of cpr in fireplace)
; cmd-shift-p sends current form to repl (equivalent of cpp in fireplace)
;
; assorted intellij commands
; https://cursive-ide.com/userguide/navigation.html
; cmd-alt-fn-f7: show usages
; alt-fn-f7: find usages
; cmd-e: recent files
; esc: focus editor window (as opposed to eg repl, project, etc)
; fn-f3 - toggle anonymous bookmark
; option-fn-f3 - add bookmark with mnemonic (use numbers)
; ctrl-<number> - jump to numbered mnemonic bookmark
; cmd-fn-f3 - view all bookmarks
;
; debugging
; option-fn-f8: toggle breakpoint
; ctrl-d - run program in debug mode
; fn-f8 - step over
; fn-f8 - step into
; shift-fn-f8 - step out


(def abc 123)

(defn thing
  [foo]
  (+ 2 1 foo))

(defn another-thing
  [some-input]
  (let [blat (thing some-input)]
    (+ 123 blat)))

(another-thing 123)

;(prn (thing 6))

(+ 123 234)

(comment

  (kalfwejklfeaae bar baz)

  (apply str (take 50 (slurp "http://www.yelp.com")))

  (thing 6)

  (+ abc 99999))



; TODO - repl has a clj/cljs toggle if you mouse over the >> bit - what's going on there?
