(ns demo3.main
  (:gen-class)
  (:require
   [com.stuartsierra.component :as component]
   [lambda.component :as lc]
   [lambda.main :as main]
   [lambda.ring :as ring]))


(defprotocol ICounter
  (-inc-page [this uri])
  (-get-page [this uri])
  (-stats [this]))


(defn new-counter []
  (let [-state (atom {})]
    (reify ICounter
      (-inc-page [this uri]
        (swap! -state update uri (fnil inc 0))
        this)
      (-get-page [this uri]
        (get @-state uri 0))
      (-stats [this]
        @-state))))


(def HELP "
Hi!

This is my lambda function for public tests. It's written in Clojure and
compiled with native image. The output binary file is run in bare AWS
environment (language-agnostic). The file communicates with AWS Lambda runtime
directly.

The Lambda uses Stuart Sierra's Component library and a Ring middleware that
turns AWS messages into Ring-compatible maps. See the source code of this demo:

https://github.com/igrishaev/lambda/blob/master/env/demo3/src/demo3/main.clj

This is what it can do:

- GET /
  Show this message;

- GET /stats
  Return statistics about how many times pages were seen;

- GET /<whatever>
  Increase in-memory counter for the current page.

You can benchmark this lambda as follows to measure RPS:

ab -n 1000 -c 200 -l https://kpryignyuxqx3wwuss7oqvox7q0yhili.lambda-url.us-east-1.on.aws/

~Ivan
")


(defn handler-info [request]
  {:status 200
   :body HELP
   :headers {"content-type" "text/plain"}})


(defn handler-stats [request counter]
  {:status 200
   :body {:stats (-stats counter)}})


(defn response-default [request counter]
  (let [{:keys [uri]}
        request

        times
        (-> counter
            (-inc-page uri)
            (-get-page uri))]

    {:status 200
     :body {:times times
            :uri uri}}))


(defn make-handler [counter]
  (fn [request]
    (let [{:keys [uri request-method]}
          request]
      (case [request-method uri]

        [:get "/"]
        (handler-info request)

        [:get "/stats"]
        (handler-stats request counter)

        (response-default request counter)))))


(defrecord RingHandler [counter]
  component/Lifecycle
  (start [this]
    (-> (make-handler counter)
        (ring/wrap-json-body)
        (ring/wrap-json-response)
        (ring/wrap-gzip)
        (ring/wrap-ring-exception)
        (ring/wrap-ring-event))))


;;
;; Must be a function but not a top-level static variable.
;; Otherwise, you'll get weird behaviour with native-image.
;;

(defn make-system []
  (component/system-map

   :counter
   (new-counter)

   :handler
   (-> {}
       (map->RingHandler)
       (component/using [:counter]))

   :lambda
   (-> (lc/lambda)
       (component/using [:handler]))))


(defn -main [& _]
  (-> (make-system)
      (component/start)))
