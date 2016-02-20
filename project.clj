(defproject mortgage "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/test.check "0.9.0"]
                 [figwheel-sidecar "0.5.0"]
                 [prismatic/schema "1.0.4"]
                 [reagent "0.6.0-alpha"]]
  :plugins [[lein-cljsbuild "1.1.2" :exclusions [[org.clojure/clojure]]]]
  :source-paths ["src"
                 "script"]
  :target-path "target/%s"
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :figwheel {:open-file-command "open-in-intellij"
             :css-dirs          ["resources/public/css"]}
  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src"]
                        :figwheel     {:on-jsload "mortgage.core/main"}
                        :compiler     {:main                 mortgage.core
                                       :asset-path           "js/compiled/out"
                                       :output-to            "resources/public/js/compiled/mortgage.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :source-map-timestamp true}}
                       {:id           "min"
                        :source-paths ["src"]
                        :compiler     {:output-to     "resources/public/js/compiled/mortgage.js"
                                       :optimizations :advanced
                                       :pretty-print  false}}]})
