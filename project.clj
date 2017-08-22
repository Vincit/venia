(defproject vincit/venia "0.2.2"
  :description "Clojure(Script) graphql client"
  :url "https://github.com/Vincit/venia"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17" :scope "provided"]
                 [org.clojure/clojurescript "1.9.908" :scope "provided"]]
  :plugins [[lein-doo "0.1.7"]]
  :clean-targets ^{:protect false} ["resources" "target"]
  :aliases {"test" ["do" "test" ["doo" "once" "phantom" "test"]]}
  :cljsbuild {:builds [{:id           "test"
                        :source-paths ["src" "test"]
                        :compiler     {:output-to     "resources/test/js/unit-test.js"
                                       :process-shim  false
                                       :main          venia.runner
                                       :optimizations :none
                                       :pretty-print  true
                                       :output-dir    "resources/test/js/gen/out"}}]})
