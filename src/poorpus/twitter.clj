(ns poorpus.twitter
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [twitter.oauth :as oauth]
            [twitter.callbacks.handlers :as handler]
            [twitter.api.streaming :as stream]
            [poorpus.playback :as fake])
  (:import twitter.callbacks.protocols.AsyncStreamingCallback))

(def twitter-creds-format "[app-key app-secret user-token user-token-secret]")

(def
  ^{:doc (str "Twitter creds format=" twitter-creds-format)
    :private true}
  twitter-creds (if-let [creds (io/resource "twitter-creds")]
                  (apply oauth/make-oauth-creds (read-string (slurp creds)))
                  (throw (Exception. (str "Missing 'twitter-creds' resource. "
                                          "Required format, including square brackets: "
                                          twitter-creds-format)))))

(defn ^:private handle
  "Turn a JSON chunk of async tweetness into a hash-map"
  [handler]
  (fn [response baos]
    (try
      (handler (json/read-json (str baos)))
      (catch Throwable ignored))))

(defn filter-tweets*
  "Invoke a twitter-api streaming connection for a comma-delimited
   statuses filter string"
  [filter handler]
  (let [callback (AsyncStreamingCallback.
                  (handle handler)
                  (comp println handler/response-return-everything)
                  handler/exception-print)]
    (stream/statuses-filter :params {:track filter}
                            :oauth-creds twitter-creds
                            :callbacks callback)))

(defn filter-tweets
  [filter handler]
  (try
    (let [stream (filter-tweets* filter handler)]
      (if (realized? (:done stream))
        (fake/filter-tweets filter handler)
        stream))))

(defn close
  [stream]
  (if stream
    (if-let [cancel (:cancel (meta stream))]
      (cancel)
      (fake/close stream))))
