(ns api-live-tests.core-test2
  (:require [midje.sweet :refer :all]
            [clojure.test :refer :all]
            [api-live-tests.core :refer :all]
            [api-live-tests.generators :refer [actions app-gen generate-app journey-gen journey-gen-two journey-gen-three]]
            [clojure.test.check.properties :as prop]
            ;[clojure.pprint :refer [pprint]]
            [api-live-tests.api :as api :refer [upload-and-create-app delete-app
                                                install-app get-installations
                                                get-owned-apps
                                                destroy-all-apps
                                                uninstall-app]]
            [clojure.test.check.generators :as gen]
            [clojure.tools.trace :refer [trace-ns]]

            [com.gfredericks.test.chuck.generators :as chuck-gen]))

(def number-of-journeys-to-take
  (try
    (Integer. (System/getenv "NUMBER_OF_JOURNEYS_TO_TAKE"))
    (catch NumberFormatException _
      1)))

(def seed
  (try
    (Integer. (System/getenv "SEED"))
    (catch NumberFormatException _
      nil)))

(trace-ns 'api-live-tests.api)
(trace-ns 'api-live-tests.core-test2)

(defn assert-world-state-is-correct [state]
  (let [expected-apps (:apps state)
        real-apps (get-owned-apps)
        expected-installations (:installations state)
        real-installations (get-installations)]
    (is (= (count real-apps) (count expected-apps)))
    (is (= (count real-installations) (count expected-installations)))
    (println "Number of apps and installations is as expected!")
    (println)))

; instance of (journey-gen1 1)
; get this by running (jg 1)
(def journey-gen-three3 (let
                         [state1 {}]
                         (chuck-gen/for
                           [action1
                            (gen/such-that
                              (fn [action] ((:possibility-check action) state1))
                              (gen/elements actions)
                              50)
                            thing1
                            ((:generator action1) state1)
                            :let
                            [state2 ((:transform action1) state1 thing1)]]
                           [[action1 thing1]])))


(defn journey-can-be-completed? [journey]
  (destroy-all-apps)

  ;(println "Undertaking journey:")
  ;(println (map (comp :name first) journey))
  ;(pprint "Undertaking journey:")
  ;(pprint (map (comp :name first) journey))
  ;(println)
  (print "hello\n")

  (print "hello\n")
  (do (reduce (fn [state [action thing]]
            (println (str "doing step…" (:name action)))

            (let [{:keys [transform perform]} action
                  expected-new-state (perform state (transform state thing) thing)]
              (doto expected-new-state
                assert-world-state-is-correct)))
          {}
          journey))

  (println "hello"))

(facts "its a journey" :integration
       (fact "that it is happening" :integration
             ;(prop/for-all [journey journey-gen-three3]
             ;              (journey-can-be-completed? journey))
             (journey-can-be-completed? journey-gen-three)
             (println "greetings")
             1 => 1))