(ns api-live-tests.slack
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

(defn send-to-slack
  "Sends a simple message to slack using an 'incoming webhook'.
   url will be of form: https://myapp.slack.com/services/hooks/incoming-webhook?token=mytoken .
   (Exact url you should use will appear on the slack integration page)
   text will be any valid message.
   This implementation could be expanded if you wanted to specify channel, username, etc.
   For more information see:
   https://my.slack.com/services/new/incoming-webhook . (You'll need a slack account
   to see that)"
  [url text emoji username]
  (client/post url {:form-params {:payload (json/write-str {:text text :icon_emoji emoji :username username})}}))

(defn -main [webhookurl text account]
  (if (= account "dev")
    (send-to-slack webhookurl text ":chicken:" "api-live-tests")
    (send-to-slack webhookurl text ":rooster:" "api-live-tests")))