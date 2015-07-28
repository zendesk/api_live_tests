(ns api-live-tests.core
  (:gen-class)
  (:require [clj-time.core :as t]
            [api-live-tests.api :as api]))

(defn install-via-api [app-id installation-name]
  (let [installation-id (api/install-non-reqs-app app-id installation-name)
        _ (println "installed app")
        _ (api/uninstall-app installation-id)]
    (println "uninstalled app")))

(defn -main [& args]
  (install-via-api 6183 "Installation name"))
