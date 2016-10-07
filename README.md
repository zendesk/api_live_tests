To run:

NUMBER_OF_JOURNEYS_TO_TAKE=5 API_EMAIL=xli@zendesk.com API_TOKEN=<secret> API_URL=https://apps-manage-professional.zd-staging.com/api/v2 lein midje api-live-tests.core-test2 

runs the tests

To post to slack:

lein run -m api-live-tests.slack <secretwebhookurl> "hi" "dev"

sort of works.  Pending -- getting a webhook to company slack channel (rather than my private one).


Debugging:

lein repl
(require '[midje.sweet :refer :all])
(require '[clojure.test :refer :all])
(require '[api-live-tests.core :refer :all])
(require '[api-live-tests.generators :refer [app-gen generate-app journey-gen journey-gen-two]])
(require '[clojure.test.check.properties :as prop])
(require '[api-live-tests.api :as api :refer [upload-and-create-app delete-app install-app get-installations get-owned-apps destroy-all-apps uninstall-app]])
(require '[clojure.test.check.generators :as gen])
(require '[clojure.tools.trace :refer [trace-ns]])
