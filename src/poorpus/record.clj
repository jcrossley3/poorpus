(ns poorpus.record
  (:require [clojure.java.io :as io]
            [poorpus.twitter :as twitter]
            [clj-http.client :as http]
            [immutant.messaging :as msg]))

(defn tweet-saver
  [path]
  (fn [tweet]
    (println "Tweet" (:id_str tweet))
    (spit (str path "/" (:id_str tweet)) tweet)
    tweet))

(defn url-saver
  [path]
  (fn [m]
    (println "URL" (first (:trace-redirects m)))
    (if-let [url (first (:trace-redirects m))]
      (spit (str path "/" (clj-http.util/url-encode url)) m))
    m))

(defn url-filter
  "Returns a function that parses a tweet for a URL and, if found,
   invokes handler with it"
  [handler]
  (fn [{text :text}]
    (when-let [url (and text (re-find #"http://[\w/.-]+" text))]
      (handler url))))

(defn fetch
  [url]
  (try
    (http/get url {:socket-timeout 10000 :conn-timeout 10000})
    (catch Exception e (println "WARN:" (.getMessage e)) {})))

(defn start
  [path filter]
  (let [tweets (io/file path "tweets")
        urls (io/file path "urls")]
    (.mkdirs tweets)
    (.mkdirs urls)
    (msg/start "queue.tweets")
    (msg/start "queue.urls")
    [(msg/listen "queue.urls" (comp (url-saver urls) fetch) :concurrency 10)
     (msg/listen "queue.tweets" (comp (url-filter #(msg/publish "queue.urls" %)) (tweet-saver tweets)))
     (twitter/filter-tweets* filter #(msg/publish "queue.tweets" %))]))

(defn stop
  [[h1 h2 stream]]
  (twitter/close stream)
  (msg/unlisten h2)
  (msg/unlisten h1)
  (msg/stop "queue.urls" :force true)
  (msg/stop "queue.tweets" :force true))