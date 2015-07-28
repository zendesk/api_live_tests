(defproject api-live-tests "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main api-live-tests.core
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [arohner/clj-webdriver "0.7.0-SNAPSHOT"]
                 [prismatic/plumbing "0.4.4"]
                 [org.clojure/data.csv "0.1.2"]
                 [clj-http "2.0.0"]
                 [clj-statsd "0.3.10"]
                 [org.clojure/test.check "0.7.0"]
                 [org.clojure/tools.trace "0.7.8"]
                 [clj-time "0.10.0"]])
