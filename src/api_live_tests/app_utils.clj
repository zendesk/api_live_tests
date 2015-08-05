(ns api-live-tests.app-utils
  (:require [api-live-tests.api :as api :refer [upload-and-create-app destroy-all-apps install-app]]
            [clojure.java.io :refer [file make-parents]]
            [clojure.java.shell :refer [sh]]
            [cheshire.core :refer [parse-string generate-string]]))

(defn mkdir [path] (.mkdir (java.io.File. path)))

(defn mk-tmp-dir!
  "Creates a unique temporary directory on the filesystem. Typically in /tmp on
  *NIX systems. Returns a File object pointing to the new directory. Raises an
  exception if the directory couldn't be created after 10000 tries.

  https://gist.github.com/samaaron/1398198
  "
  []
  (let [base-dir (file (System/getProperty "java.io.tmpdir"))
        base-name (str (System/currentTimeMillis) "-" (long (rand 1000000000)) "-")
        tmp-base (str (if (.endsWith (.getPath base-dir) "/")
                        (.getPath base-dir)
                        (str (.getPath base-dir) "/"))
                      base-name)
        max-attempts 10000]
    (loop [num-attempts 1]
      (if (= num-attempts max-attempts)
        (throw (Exception. (str "Failed to create temporary directory after " max-attempts " attempts.")))
        (let [tmp-dir-name (str tmp-base num-attempts)
              tmp-dir (file tmp-dir-name)]
          (if (.mkdir tmp-dir)
            tmp-dir
            (recur (inc num-attempts))))))))


(defn hyphens-to-camel-case-name [method-name]
  "e.g. hello-world -> helloWorld"
  (clojure.string/replace method-name #"-(\w)"
                          #(clojure.string/upper-case (second %1))))


(defn keys-to-camel-case [data]
  (if (map? data)
    (into {}
          (for [[k v] data]
            [(hyphens-to-camel-case-name (name k)) (keys-to-camel-case v)]))
    data))


(defn serialize-app-to-tmpdir! [{:keys [translations manifest requirements app-js] :as app}]
  (let [dir (mk-tmp-dir!)
        translation-files (map #(file dir (str "translations/" % ".json")) translations)
        filenames-to-data {"manifest.json" (keys-to-camel-case manifest)
                           "requirements.json" requirements}]
    (doseq [[filename data] filenames-to-data]
      (spit (file dir filename)
            (generate-string data
                             {:pretty true})))
    (doseq [translation-file translation-files]
      (make-parents translation-file)
      (spit translation-file (generate-string {})))
    (when app-js (.createNewFile (file dir "app.js")))
    dir))

(defn valid-app? [app]
  (let [app-dir (serialize-app-to-tmpdir! app)
        zat-validate (sh "zat" "validate" :dir app-dir)]
    (= 0 (:exit zat-validate))))

(defn zip [dir]
  (sh "zip" "-r" "app" "." :dir dir)
  (file dir "app.zip"))


(defn create-and-install-app [app]
  (println "\n\n")
  (println "Creating / installing app:")
  (clojure.pprint/pprint app)
  (println "\n")
  (destroy-all-apps)
  (let  [app-dir (serialize-app-to-tmpdir! app)
         zip-file (zip app-dir)
         app-name (:app-name app)
         app-id (upload-and-create-app zip-file app-name)
         install-id (install-app app-id "sample title")]
    {:app-id app-id :install-id install-id}))


(defn app-installs? [app]
  (let [{:keys [app-id install-id]} (create-and-install-app app)]
    (and (pos? install-id)
         (api/uninstall-app install-id)
         (api/delete-app app-id))))
