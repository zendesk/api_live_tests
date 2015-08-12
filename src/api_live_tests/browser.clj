(ns api-live-tests.browser
  (:use clj-webdriver.taxi)
  (:require [clj-webdriver.remote.server :only [new-remote-session stop]]))

(def d
  (second (clj-webdriver.remote.server/new-remote-session
            {:host     "redacted:redacted@localhost"
             :port     4445
             :existing true}
            {:capabilities {:browserName "chrome"
                            :platform "WIN7"
                            :idleTimeout 400}})))

(defn login []
  (-> d
      (to "https://alistair.zd-staging.com/agent")
      ))

(defn install-app [app-id installation-name]
  (with-redefs [*driver* d]
    (implicit-wait 5000)
    (delete-all-cookies)
    (add-cookie {:name  "_zendesk_shared_session"
                 :value "redacted"
                 })
    (to "https://alistair.zd-staging.com/apps")
    (wait-until (fn [] (find-element d {:css (str ".app_" app-id " img")})))
    (click (str ".app_" app-id " img"))
    (wait-until (fn [] (find-element d {:css "a.btn.install"})))
    (click "a.btn.install")
    (clear "#settings_name")
    (input-text "#settings_name" installation-name)
    (submit ".app form")))

(defn get-installation-on-manage-page [installation-name]
  (println "doing get installation")
  (first (filter (fn [e] (= installation-name
                            (first (clojure.string/split (text e) #"\n"))))
                 (find-elements {:css "li.installation"}))))

(defn uninstall-app [installation-name]
  (println "start uninstall process")
  (with-redefs [*driver* d]
    (println "waiting for li.installations to not be empty")
    (wait-until (fn [] (not (empty? (find-elements {:css "li.installation"})))))
    (println "they're apparently not empty now")
    (let [install (get-installation-on-manage-page installation-name)]
      (println "found instllation element")
      (click (find-element-under install {:css "button.dropdown-toggle"}))
      (println "clicked dropdown")
      (click (find-element-under install {:css "li.uninstall a"})))
    (println "clicked uninstall")
    (wait-until (fn [] (enabled? "#uninstallAppConfirmation button.agree")))
    (click "#uninstallAppConfirmation button.agree")))



(defn install-in-brower-with-timestamp [app-id installation-name]
  "Produces a vector like this:

   [:installed #<DateTime 2014-10-12T20:15:34.873Z>
    :uninstalled #<DateTime 2014-10-12T20:15:36.914Z>]"
  (let [installation-id (install-app app-id installation-name)
        after-install (t/now)
        _ (uninstall-app installation-name)
        after-uninstall (t/now)]
    [:installed after-install
     :uninstalled after-uninstall]))