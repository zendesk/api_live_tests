(ns api-live-tests.generators
  (:require [clojure.test.check.generators :refer [sample] :as gen]
            [com.gfredericks.test.chuck.generators :as chuck-gen]
            [clojure.tools.trace]
            [miner.herbert.generators :as hg]
            [miner.herbert :as h]
            [backtick :refer [template]]
            [api-live-tests.app-utils :refer [valid-app? app-installs?
                                              zip serialize-app-to-tmpdir!]]))

(def not-empty-string '(str #"[A-Za-z0-9][A-Za-z0-9 ]+"))

(def locale-gen (hg/generator '(or "en" "jp" "de")))
(def author-gen
  (hg/generator (template {:name  ~not-empty-string
                           :email ~not-empty-string})))

(defn manifest-gen [requirements-only]
  (chuck-gen/for [default-locale locale-gen
                  author author-gen
                  private gen/boolean
                  no-template gen/boolean]
                 {:requirements-only requirements-only
                  :default-locale    default-locale
                  :location          (if requirements-only
                                       nil
                                       ["nav_bar"])
                  :author            author
                  :private           private
                  :no-template       no-template
                  :framework-version (if requirements-only
                                       nil
                                       "1.0")}))



(def ticket-fields-gen
  (hg/generator (template
                  {~not-empty-string {:type  "text"
                                      :title ~not-empty-string}})))

(def targets-gen
  (hg/generator (template
                  {~not-empty-string {:type    "email_target"
                                      :title   ~not-empty-string
                                      :email   "blah@hoo.com"
                                      :subject ~not-empty-string}})))

(defn triggers-gen [custom-field-identifiers]
  (let [field-pointers (map (partial str "custom_fields_") custom-field-identifiers)]
    (hg/generator (template
                    {~not-empty-string {:title   ~not-empty-string
                                        :all     (vec (& {"field"    (or ~@field-pointers "status")
                                                          "operator" "is"
                                                          "value"    "open"}))
                                        :actions (vec (& {"field" "priority"
                                                          "value" "high"}))}}))))


(defn no-shared-keys [& maps]
  (if (some empty? maps)
    true
    (->> maps
         (map keys)
         flatten
         (apply distinct?))))

(def requirements-gen
  (chuck-gen/for [:parallel [ticket-fields ticket-fields-gen
                             targets       targets-gen]

                  triggers (triggers-gen (keys ticket-fields))

                  :when ^{:max-tries 30} (no-shared-keys ticket-fields targets triggers)]

                 {:ticket_fields ticket-fields
                  :targets       targets
                  :triggers      triggers}))


(def app-gen
  (chuck-gen/for [:parallel [requirements requirements-gen
                             app-name (hg/generator not-empty-string)]
                  requirements-only gen/boolean
                  manifest (manifest-gen requirements-only)
                  :let [app-js (not requirements-only)]]
   {:manifest     manifest
    :requirements requirements
    :templates    []
    :app-name     app-name
    :app-js       app-js
    :translations [(:default-locale manifest)]
    :assets       []}))


(defn generate-app [] (rand-nth (sample app-gen 20)))
