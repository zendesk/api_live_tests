(ns api-live-tests.core-test
  (:require [clojure.test :refer :all]
            [api-live-tests.core :refer :all]
            [api-live-tests.app-utils :refer [app-installs?]]
            [api-live-tests.generators :refer [app-gen]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check :as tc]))

(def number-of-apps-to-try (or (System/getenv "NUMBER_OF_APPS_TO_TRY") 20))

(defspec apps-can-be-installed
         number-of-apps-to-try
         (prop/for-all [app app-gen]
                       (app-installs? app)))
