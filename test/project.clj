(defproject martian-test "0.1.0-SNAPSHOT"
  :description "Testing tools for martian"
  :url "https://github.com/oliyh/martian"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-modules "0.3.11"]
            [lein-cljsbuild "1.1.1"]
            [lein-doo "0.1.6"]]
  :dependencies [[martian :version]
                 [prismatic/schema-generators "0.1.0"]]
  :java-source-paths ["src"]
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.8.0"]
                                       [org.clojure/clojurescript "1.9.36"]]}
             :dev {:exclusions [[org.clojure/tools.reader]]
                   :resource-paths ["test-resources"]
                   :dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.9.36"]
                                  [prismatic/schema "1.1.2"]
                                  [org.clojure/tools.reader "0.10.0"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.mozilla/rhino "1.7.7.1"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :aliases {"test" ["do" ["clean"] ["test"] ["doo" "rhino" "test" "once"]]}
  :doo {:paths {:rhino "lein run -m org.mozilla.javascript.tools.shell.Main"}}
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/unit-test.js"
                                   :main 'martian.runner
                                   :optimizations :whitespace}}]})