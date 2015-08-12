(ns api-live-tests.core-test
  (:require [clojure.test :refer :all]
            [api-live-tests.core :refer :all]
            [api-live-tests.generators :refer [app-gen generate-app journey-gen journey-gen-two]]
            [clojure.test.check.properties :as prop]
            [clojure.pprint :refer [pprint]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [api-live-tests.api :as api :refer [upload-and-create-app delete-app
                                                install-app get-installations
                                                get-owned-apps
                                                destroy-all-apps
                                                uninstall-app]]
            [clojure.test.check.generators :as gen]
            [clojure.tools.trace :refer [trace-ns]]))

(def number-of-journeys-to-take
  (try
    (Integer. (System/getenv "NUMBER_OF_JOURNEYS_TO_TAKE"))
    (catch NumberFormatException _
      1)))

(trace-ns 'api-live-tests.api)
(trace-ns 'api-live-tests.core-test)

(defn assert-world-state-is-correct [state]
  (let [expected-apps (:apps state)
        real-apps (get-owned-apps)
        expected-installations (:installations state)
        real-installations (get-installations)]
    (is (= (count real-apps) (count expected-apps)))
    (is (= (count real-installations) (count expected-installations)))
    (println "Number of apps and installations is as expected!")
    (println)))


(defn journey-can-be-completed? [journey]
  (destroy-all-apps)

  (pprint "Undertaking journey:")
  (pprint (map (comp :name first) journey))
  (println)

  (reduce (fn [state [action thing]]
            (println (str "doing step…" (:name action)))

            (let [{:keys [transform perform]} action
                  expected-new-state (perform state (transform state thing) thing)]
              (doto expected-new-state
                assert-world-state-is-correct)))
          {}
          journey))


(defspec apps-can-be-installed
         number-of-journeys-to-take
         (prop/for-all [journey journey-gen]
                       (journey-can-be-completed? journey)))
