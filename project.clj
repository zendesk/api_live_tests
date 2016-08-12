(defproject api-live-tests "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main api-live-tests.core
  :injections [(require 'spyscope.core)]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-zendesk "0.3.0-SNAPSHOT" :exclusions [io.aviso/pretty]],
                 [com.velisco/herbert "0.7.0"],
                 [org.clojure/test.check "0.9.0"],
                 [com.gfredericks/test.chuck "0.2.7"],
                 [backtick "0.3.3"],
                 [spyscope "0.1.5"],
                 [clj-http "2.2.0"],
                 [debugger "0.2.0"]
                 ]
  ;               [org.clojure/clojure "1.7.0"]
  ;               [prismatic/plumbing "0.4.4"]
  ;               [w01fe/sniper "0.1.0"]
  ;               [org.clojure/data.csv "0.1.3"]
  ;               [clj-http "2.0.0"]
  ;               [clj-statsd "0.3.11"]
  ;               [org.clojure/test.check "0.7.0"]
  ;               [com.velisco/herbert "0.6.11"]
  ;               [com.gfredericks/test.chuck "0.1.21"]
  ;               [spyscope "0.1.5"]
  ;               [org.clojure/tools.trace "0.7.8"]
  ;               [backtick "0.3.3"]
  ;               [debugger "0.1.7"]
  ;               [clj-time "0.10.0"]])
  :profiles {:dev {:dependencies [[midje "1.8.3"]
                                  [lein-light-nrepl "0.0.18"]]
                   :plugins [[lein-midje "3.0.1"]
                             [lein-html5-docs "3.0.1"]
                             [lein-cloverage "1.0.6"]]}})
