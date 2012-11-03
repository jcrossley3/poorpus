(ns poorpus.http
  (:require [clj-http.client :as client]
            [poorpus.playback :as fake])
  (:refer-clojure :exclude (get)))

(defn get
  [url & [options]]
  (try
    (client/get url options)
    (catch Exception e
      (fake/get url options))))