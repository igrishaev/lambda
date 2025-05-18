;; TODO: subproject?
(ns lambda.component
  (:import
   (clojure.lang IFn))
  (:require
   [com.stuartsierra.component :as component]
   [lambda.log :as log]
   [lambda.main :as main]))


(set! *warn-on-reflection* true)


(defrecord LambdaHandler [^IFn handler
                          ^Thread thread]

  component/Lifecycle

  (start [this]
    (if thread
      this
      (let [thread (main/run-thread handler)]
        (log/infof "lambda handler thread started")
        (assoc this :thread thread))))

  (stop [this]
    (if thread
      (do
        (.interrupt thread)
        (log/infof "lambda handler thread interrupted")
        (.join thread)
        (log/infof "lambda handler thread joined")
        (assoc this :thread nil))
      this)))


(defn lambda
  ([]
   (map->LambdaHandler nil))

  ([handler]
   (map->LambdaHandler {:handler handler})))
