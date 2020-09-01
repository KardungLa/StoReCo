(ns storeco.core
  "StoReCo CLI"
  (:gen-class)
  (:require
   [storeco.api :as api]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]))

;; Copyright © 2020 Daniel Schlager

(def cli-opts
  [["-h" "--help"]
   ["-c" "--config PATH" "EDN file to read config options from"]
   ["-i" "--input INPUT"]
   ["-o" "--output OUTPUT"]
   ["-of" "--output-format FORMAT"]]
  )

(defn help-text [options-summary]
  (str "StoReCo

  Usage: storeco [options]

  Options:
" options-summary))

(defn -main [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-opts :no-defaults true)
        {:keys [help]} options
        config-file (api/get-config)
        options (merge (cli/get-default-options cli-opts)
                  config-file
                  (dissoc options :config))]
    (cond
      errors (println (str/join "\n" errors))
      help (println (help-text summary))
      :else (api/build options))))
