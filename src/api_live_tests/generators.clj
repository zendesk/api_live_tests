(ns api-live-tests.generators
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :refer [sample] :as gen]
            [com.gfredericks.test.chuck.generators :as chuck-gen]
            [clojure.tools.trace]
            [api-live-tests.api :as api]
            [api-live-tests.app-utils :refer [valid-app? app-installs?
                                              zip serialize-app-to-tmpdir!]]
            [clojure.test.check.properties :as prop]))

(defn hmap-gen [map-schema]
  "Takes map like {:whatever whatev-gen :another-key another-generator} and
   returns a generator."
  (gen/fmap (fn [tuple]
              (into {} (map vector (keys map-schema) tuple)))
            (apply gen/tuple (vals map-schema))))

(def not-empty-string (gen/such-that not-empty gen/string-alpha-numeric))

(def locale-gen (gen/elements ["en" "de" "jp"]))

(def author-gen (hmap-gen {:name  not-empty-string
                           :email not-empty-string}))

(def manifest-gen
  (gen/bind gen/boolean
            (fn [requirements-only]
              (hmap-gen {:requirements-only (gen/return requirements-only)
                         :default-locale    locale-gen
                         :location          (if requirements-only
                                              (gen/return nil)
                                              (gen/tuple (gen/elements ["nav_bar"])))
                         :author            author-gen
                         :private           gen/boolean
                         :no-template       gen/boolean
                         :framework-version (if requirements-only
                                              (gen/return nil)
                                              (gen/elements ["1.0"]))}))))



(def ticket-fields-gen
  (gen/map not-empty-string
           (hmap-gen {:type  (gen/return "text")
                      :title not-empty-string})))

(def targets-gen
  (gen/map not-empty-string
           (hmap-gen {:type    (gen/return "email_target")
                      :title   not-empty-string
                      :email   (gen/return "blah@hoo.com")
                      :subject not-empty-string})))

(def triggers-gen
  (gen/map not-empty-string
           (hmap-gen {:title   not-empty-string
                      :all     (gen/return [{"field"    "status"
                                             "operator" "is"
                                             "value"    "open"}])
                      :actions (gen/return [{"field"    "priority"
                                             "operator" "is"
                                             "value"    "high"}])})))

(defn no-shared-keys [& maps]
  (if (some empty? maps)
    true
    (->> maps
         (map keys)
         flatten
         (apply distinct?))))


(def requirements-gen
  (chuck-gen/for [:parallel [ticket-fields ticket-fields-gen
                             targets targets-gen
                             triggers triggers-gen]
                  :when (no-shared-keys ticket-fields targets triggers)]
                 {:ticket_fields ticket-fields
                  :targets       targets
                  :triggers      triggers}))

(def app-gen
  (gen/bind manifest-gen
            (fn [manifest]
              (hmap-gen {:manifest     (gen/return manifest)
                         :requirements requirements-gen
                         :templates    (gen/return [])
                         :app-name     (gen/such-that #(> (count %) 10) gen/string-ascii 1000)
                         :app-js       (gen/return (not (:requirements-only manifest)))
                         :translations (gen/return [(:default-locale manifest)])
                         :assets       (gen/return [])}))))

(defn generate-app [] (rand-nth (sample app-gen 20)))
