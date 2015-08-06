(ns api-live-tests.generators
  (:require [clojure.test.check.generators :refer [sample] :as gen]
            [com.gfredericks.test.chuck.generators :as chuck-gen]
            [clojure.tools.trace]
            [miner.herbert.generators :as hg]
            [backtick :refer [template]]
            [api-live-tests.app-utils :refer [valid-app? app-installs?
                                              zip serialize-app-to-tmpdir!]]))

(def not-empty-string '(str #"[A-Za-z0-9][A-Za-z0-9 ]+"))

(def locale-gen (hg/generator '(or "en" "jp" "de")))
(def author-gen
  (hg/generator (template {:name  ~not-empty-string
                           :email ~not-empty-string})))

(defn manifest-gen [requirements-only parameters]
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
                  :parameters        parameters
                  :no-template       no-template
                  :framework-version (if requirements-only
                                       nil
                                       "1.0")}))



(def ticket-fields-gen
  (hg/generator (template
                  {~not-empty-string {:type  "checkbox"
                                      :tag (str #"[A-Za-z0-9]+")
                                      :title ~not-empty-string}})))

(defn targets-gen [parameters]
  (let [params-to-interpolate (map (comp (partial format "{{settings.%s}}") :name) parameters)]
    (hg/generator (template
                    {~not-empty-string {:type    "email_target"
                                        :title   ~not-empty-string
                                        :email   "blah@hoo.com"
                                        :subject (or ~not-empty-string
                                                     ~@params-to-interpolate)}}))))

(defn triggers-gen [custom-field-identifiers]
  (let [field-pointers (map (partial str "custom_fields_") custom-field-identifiers)]
    (hg/generator (template
                    {~not-empty-string {:title      ~not-empty-string
                                        :conditions {:all (vec (& (or {"field"    "status"
                                                                       "operator" "is"
                                                                       "value"    "open"}
                                                                      {"field"    (or ~@field-pointers)
                                                                       "operator" "is"
                                                                       "value"    (or "true" "false")})))}
                                        :actions    (vec (& {"field" "priority"
                                                             "value" "high"}))}}))))


(defn no-shared-keys [& maps]
  (if (some empty? maps)
    true
    (->> maps
         (map keys)
         flatten
         (apply distinct?))))

(defn requirements-gen [parameters]
  (chuck-gen/for [:parallel [ticket-fields ticket-fields-gen
                             targets (targets-gen parameters)]

                  triggers (triggers-gen (keys ticket-fields))

                  :when ^{:max-tries 30} (no-shared-keys ticket-fields targets triggers)]

                 {:ticket_fields ticket-fields
                  :targets       targets
                  :triggers      triggers}))




(def parameters-gen
  (hg/generator (template
                  [{:type     "text"
                    :name     ~not-empty-string
                    :required bool
                    :secure   bool
                    :default  ~not-empty-string}])))


(def app-gen
  (chuck-gen/for [parameters parameters-gen
                  :parallel [requirements (requirements-gen parameters)
                             app-name (hg/generator not-empty-string)]
                  requirements-only gen/boolean
                  manifest (manifest-gen requirements-only parameters)
                  :let [app-js (not requirements-only)]]
                 {:manifest     manifest
                  :requirements requirements
                  :templates    []
                  :app-name     app-name
                  :app-js       app-js
                  :translations [(:default-locale manifest)]
                  :assets       []}))


(defn install-gen [app-id]
  (hg/generator (template {:app-id   ~app-id
                           :settings {:name ~not-empty-string}
                           :enabled  bool})))


(defn generate-app [] (rand-nth (sample app-gen 20)))
(defn generate-installation [app-id] (rand-nth (sample (install-gen app-id) 20)))


; TODO: if app has settings, install with values for those settings
