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
        (swap! -state update uri (fnil inc 0)))
      (-get-page [this uri]
        (get @-state uri 0))
      (-stats [this]
        @-state))))


(deftype Counter []
  component/Lifecycle
  (start [this]
    (new-counter))
  (stop [this]
    this))


(defn handler-index [request counter]
  {:status 200
   :body {:stats (-stats counter)}})


(defn handler-hello [request]
  {:status 200
   :body {:page "hello"}})


(defn response-default [request counter]
  (let [{:keys [uri]}
        request]
    (-inc-page counter uri)
    {:status 200
     :body {:seen true
            :uri uri}}))


(defn make-handler [counter]
  (fn [request]
    (let [{:keys [uri request-method]}
          request]
      (case [request-method uri]

        [:get "/"]
        (handler-index request counter)

        [:get "/hello"]
        (handler-hello request)

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


(defn make-system []
  (component/system-map

   :counter
   (new Counter)

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
