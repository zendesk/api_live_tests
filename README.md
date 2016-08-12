To run:

NUMBER_OF_JOURNEYS_TO_TAKE=5 API_EMAIL=xli@zendesk.com API_TOKEN=<secret> API_URL=https://apps-manage-professional.zd-staging.com/api/v2 lein midje api-live-tests.core-test2 

runs the tests

To post to slack:

lein run -m api-live-tests.slack <secretwebhookurl> "hi" "dev"

sort of works.  Pending -- getting a webhook to company slack channel (rather than my private one).