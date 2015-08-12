(ns api-live-tests.generators
  (:use [debugger.core])
  (:require [clojure.test.check.generators :refer [sample] :as gen]
            [com.gfredericks.test.chuck.generators :as chuck-gen]
            [clojure.tools.trace]
            [api-live-tests.app-utils :refer [serialize-app-to-tmpdir! zip]]
            [api-live-tests.api :as api :refer [upload-and-create-app delete-app
                                                install-app get-installations
                                                get-owned-apps
                                                destroy-all-apps
                                                uninstall-app]]
            [miner.herbert.generators :as hg]
            [backtick :refer [template]]
            [clojure.pprint :refer [pprint]]
            [api-live-tests.app-utils :refer [zip serialize-app-to-tmpdir!]]))

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

(def ticket-field-gen
  (chuck-gen/for [tag (chuck-gen/string-from-regex #"[A-Za-z0-9]{8,12}")]
    [tag {:type  "checkbox"
          :tag   tag
          :title tag}]))

(def ticket-fields-gen
  (chuck-gen/for [ticket-fields (gen/vector ticket-field-gen)]
    (into {} ticket-fields)))

(defn targets-gen [parameters]
  (let [params-to-interpolate (map (comp (partial format "{{settings.%s}}") :name) parameters)]
    (hg/generator (template
                   {~not-empty-string {:type    "email_target"
                                       :title   ~not-empty-string
                                       :email   "blah@hoo.com"
                                       :subject (or ~not-empty-string
                                                    ~@params-to-interpolate)}}))))

(defn triggers-gen [custom-field-identifiers]
  (let [field-pointers (map (partial str "custom_fields_") custom-field-identifiers)
        condition (if (seq field-pointers)
                    (template (or {"field"    "priority"
                                   "operator" "is"
                                   "value"    "high"}
                                  {"field"    (or ~@field-pointers)
                                   "operator" "is"
                                   "value"    (or "true" "false")}))
                    {"field"    "status"
                     "operator" "is"
                     "value"    "open"})]
    (hg/generator (template
                   {~not-empty-string {:title      ~not-empty-string
                                       :conditions {:all (vec (& ~condition))}
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

                  :when ^{:max-tries 1000} (no-shared-keys ticket-fields targets triggers)]

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

(defn install-gen [specific-app-gen]
  (chuck-gen/for [settings (hg/generator (template {:name ~not-empty-string}))
                  app specific-app-gen
                  enabled gen/boolean]
    {:app      app
     :settings settings
     :enabled  enabled}))

(defn generate-app [] (rand-nth (sample app-gen 2)))
(defn generate-installation [app] (rand-nth (sample (install-gen app) 2)))

(defn requires-ticket-fields? [app]
  (-> app :requirements :ticket_fields empty? not))

(defn lazy-contains? [col key]
  (some #{key} col))

(def actions
  #{{:name              :create-app
     :possibility-check (fn [state]
                          true)
     :generator         (fn [state] app-gen)
     :perform           (fn [before-state expected-state app-to-create]
                          (let [app-dir (serialize-app-to-tmpdir! app-to-create)
                                zip-file (zip app-dir)
                                app-name (:app-name app-to-create)
                                app-id (upload-and-create-app zip-file app-name)]
                            (assoc expected-state :apps
                                   (map (fn [app]
                                          (if (= app app-to-create)
                                            (assoc app :id app-id)
                                            app))
                                        (:apps expected-state)))))
     :transform         (fn [state app]
                          (update-in state [:apps] conj app))}
    ; TODO: check response is correct
    ;{:name              :delete-app
    ; :generator         (fn [state]
    ;                      (gen/elements (:apps state)))
    ; :possibility-check (fn [state]
    ;                      (> (count (:apps state)) 0))
    ; :thing-creator     (fn [state] (rand-nth (:apps state)))
    ; :perform           (fn [before-state expected-state app-to-delete]
    ;                      (pprint expected-state)
    ;                      (let [created-app (first (filter (fn [app]
    ;                                                         (= app-to-delete
    ;                                                            (dissoc app :id)))
    ;                                                       (:apps before-state)))]
    ;                        (delete-app (:id created-app)))
    ;                      expected-state)
    ; :transform         (fn [state app]
    ;                      (let [app-id (:id (rand-nth (:apps state)))]
    ;                        (-> state
    ;                            (update-in [:apps]
    ;                                       (partial remove #(= (:id %) app-id)))
    ;                            (update-in [:installations]
    ;                                       (partial remove #(= (:app-id %) app-id))))))}
    ; gonna have some sort of assert associated with it?
    ; no! “types” are, like app or installation or whatever
    ; maybe will even split out into separate map at some stage
    {:name              :install-app
     :possibility-check (fn [state]
                          ; if there's an app without required ticket fields
                          ; or if there's one with required ticket fields that hasn't been installed yet
                          (let [apps (:apps state)
                                there-are-apps (seq apps)]
                            (and there-are-apps
                                 (let [apps-that-requires-ticket-field (filter requires-ticket-fields?
                                                                               apps)]
                                   (or (empty? apps-that-requires-ticket-field)
                                       (let [installed-apps (map :app (:installations state))

                                             installed? (partial lazy-contains? installed-apps)]
                                         (not-every? installed?
                                                     apps-that-requires-ticket-field)))))))
     :generator         (fn [state]
                          (let [apps (:apps state)
                                appropriate-app? (fn [app]
                                                   (let [installed-apps (map :app (:installations state))
                                                         not-installed? (fn [app]
                                                                          (not (lazy-contains? installed-apps app)))
                                                         can-be-installed-twice? (comp not requires-ticket-fields?)]
                                                     (or  (not-installed? app)
                                                          (can-be-installed-twice? app))))]
                            (install-gen (gen/elements (filter appropriate-app? apps)))))
     :thing-creator     (fn [state]
                          (generate-installation (rand-nth (:apps state))))
     :perform           (fn [before-state expected-state installation-to-create]
                          (let [app-to-install (first (filter (fn [app]
                                                                (= (:app installation-to-create)
                                                                   (dissoc app :id)))
                                                              (:apps expected-state)))
                                installation-id (install-app (-> installation-to-create
                                                                 (dissoc :app)
                                                                 (assoc :app-id (:id app-to-install))))]
                            (assoc expected-state :installations
                                   (map (fn [installation]
                                          (if (= installation installation-to-create)
                                            (assoc installation :id installation-id)
                                            installation))
                                        (:installations expected-state)))))
     :transform         (fn [state installation]
                          (update-in state [:installations] conj installation))}
    ;{:name              :uninstall-app
    ; :thing-creator     (fn [state] (rand-nth (:installations state)))
    ; :possibility-check (fn [state]
    ;                      (> (count (:installations state)) 0))
    ; :transform         (fn [state installation]
    ;                      (let [install-id (:id (rand-nth (:installations state)))]
    ;                        (update-in state [:installations]
    ;                                   (partial remove #(= (:id %) install-id)))))}
})

(defn jg [steps]
  (let [step-sym (fn [sym-name step-num]
                   (symbol (str sym-name step-num)))
        gen-step (fn [step]
                   (let [action (step-sym "action" step)
                         state (step-sym "state" step)
                         thing (step-sym "thing" step)]
                     (template [~action (gen/elements actions)
                                :when ((:possibility-check ~action) ~state)
                                ~thing ((:generator ~action) ~state)
                                :let [~(step-sym "state" (inc step)) ((:transform ~action) ~state ~thing)]])))
        step-list (range 1 (inc steps))]
    (template
     (let [state1 {}]
       (chuck-gen/for [~@(apply concat (map gen-step step-list))]

         ~(mapv (fn [step]
                  [(step-sym "action" step) (step-sym "thing" step)])
                step-list))))))

(defmacro journey-gen1 [steps]
  (jg steps))

(def journey-gen (journey-gen1 5))
(def journey-gen-two (journey-gen1 2))

; TODO: if app has settings, install with values for those settings
