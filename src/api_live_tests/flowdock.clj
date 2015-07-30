(ns api-live-tests.flowdock
  (:require [clj-http.client :as client]))

(def fd-token (System/getenv "FD_TOKEN"))
(defn post-fd-message [message]
  (client/post (str "https://api.flowdock.com/v1/messages/chat/" fd-token)
               {:form-params  {:content            message
                               :external_user_name "Jenkins"}
                :content-type :json}))


(defn -main [message]
  (post-fd-message message))