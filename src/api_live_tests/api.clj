(ns api-live-tests.api
  (:use [clj-zendesk.core])
  (:require [clj-http.client :as client]
            [clojure.tools.trace :refer [trace-ns]]))

(def token
  (System/getenv "API_TOKEN"))

(def api-url
  (System/getenv "API_URL"))

(defn apps-url
  [path] (str api-url "/apps" path))
;
(def auth-creds
  [(str (System/getenv "API_EMAIL") "/token") token])



(defn create-upload [app-zip-filename]
  (let [response (client/post (apps-url "/uploads.json")
                              {:basic-auth auth-creds
                               :as :json
                               :multipart [{:name "uploaded_data"
                                            :content (clojure.java.io/file app-zip-filename)}]})]
    (get-in response [:body :id])))

(defn create-app [upload-id app-name]
  (let [response (client/post (str api-url "/apps.json")
                              {:basic-auth auth-creds
                               :form-params {:name app-name
                                             :short_description "a description"
                                             :upload_id upload-id}
                               :content-type :json
                               :as :json})]
    (get-in response [:body :job_id])))

(defn get-job-status [job-id]
  (let [response (client/get (apps-url (str "/job_statuses/" job-id ".json"))
                             {:basic-auth auth-creds
                              :as :json})]
    (:body response)))

(defn get-installation-job-status [job-id]
  (let [response (client/get (apps-url (str "/installations/job_statuses/" job-id ".json"))
                             {:basic-auth auth-creds
                              :as :json})]
    (:body response)))

(defn app-id-when-job-completed [job-id]
  (Thread/sleep 2000)
  (loop [job-status (get-job-status job-id)]
    (Thread/sleep 2000)
    (case (:status job-status)
      "completed" (:app_id job-status)
      "failed" (do
                 (println "FAILURE FAILURE FAILURE")
                 (println (str "Job failed: " (:message job-status)))
                 (System/exit 1))
      (recur (get-job-status job-id)))))

(defn upload-and-create-app [app-zip-filename app-name]
  (let [upload-id (create-upload app-zip-filename)
        job-status-id (create-app upload-id app-name)]
    (app-id-when-job-completed job-status-id)))

(defn installation-id-when-job-completed [job-id]
  (Thread/sleep 2000)
  (loop [job-status (get-installation-job-status job-id)]
    (Thread/sleep 2000)
    (case (:status job-status)
      "completed" (:installation_id job-status)
      "failed" (do
                 (println "FAILURE FAILURE FAILURE")
                 (println (str "Job failed: " (:message job-status)))
                 (System/exit 1))
      (recur (get-installation-job-status job-id)))))

(defn start-app-install-map [http-options]
  (let [response (client/post (apps-url "/installations.json")
                              (merge
                                {:basic-auth auth-creds
                                 :as :json}
                                http-options))]
    (-> response :body :pending_job_id)))

(defn start-app-install [installation]
  (let [{:keys [settings app-id enabled]} installation
        response (client/post (apps-url "/installations.json")
                              {:basic-auth auth-creds
                               :form-params {:settings settings
                                             :enabled enabled
                                             :app_id app-id}
                               :content-type :json
                               :as :json})]
    (-> response :body :pending_job_id)))

(defn install-non-reqs-app [app-id installation-name]
  (let [response (client/post (apps-url "/installations.json")
                              {:basic-auth auth-creds
                               :form-params {:settings {:name installation-name}
                                             :app_id app-id}
                               :content-type :json
                               :as :json})]
    (-> response :body :id)))

(defn install-app [installation]
  (let [job-id (start-app-install installation)]
    (installation-id-when-job-completed job-id)))


(defn uninstall-app [installation-id]
  (client/delete (apps-url (str "/installations/" installation-id ".json"))
                 {:basic-auth auth-creds
                  :content-type :json
                  :as :json}))


(defn delete-app [app-id]
  (client/delete (apps-url (str "/" app-id ".json"))
                 {:basic-auth auth-creds
                  :content-type :json
                  :as :json}))


(defn get-owned-apps []
  (let [response (client/get (apps-url (str "/owned.json"))
                             {:basic-auth auth-creds
                              :as :json})]
    (get-in response [:body :apps])))


(defn get-installations []
  (let [response (client/get (apps-url (str "/installations.json"))
                             {:basic-auth auth-creds
                              :as :json})]
    (get-in response [:body :installations])))


(defn destroy-all-apps []
  (doseq [app-id (map :id (get-owned-apps))]
    (delete-app app-id)))

(defn destroy-all-ticket-fields []
  (doseq [ticketfield (get-all TicketFields)]
    (when (and (not (:system-field-options ticketfield)) (:removable ticketfield)) (delete-one TicketField (:id ticketfield)))))

(defn destroy-all-triggers []
  (doseq [trigger (get-all Triggers)]
    (delete-one Trigger (:id trigger))))

(defn destroy-all-targets []
  (dorun (pmap #(delete-one Target (:id %)) (get-all Targets))))

