(ns lambda.component
  "
  Stuart Sierra's Component integration. Provides a function to build
  a component that processes Lambda messages in a separate thread.
  "
  (:import
   (clojure.lang IFn))
  (:require
   [lambda.log :as log]
   [lambda.main :as main]))


(set! *warn-on-reflection* true)


(defrecord LambdaHandler
    [;; deps
     ^IFn handler

     ;; runtime
     ^Thread -thread])


(defn with-component-meta
  "
  Extend via metadata to not depend on the component library.
  "
  [component]
  (with-meta component
    {'com.stuartsierra.component/start
     (fn [{:as this :keys [-thread handler]}]
       (if -thread
         this
         (let [-thread (main/run-thread handler)]
           (log/infof "lambda handler thread started")
           (assoc this :-thread -thread))))

     'com.stuartsierra.component/stop
     (fn [{:as this :keys [^Thread -thread]}]
       (if -thread
         (do
           (.interrupt -thread)
           (log/infof "lambda handler thread interrupted")
           (.join -thread)
           (log/infof "lambda handler thread joined")
           (assoc this :-thread nil))
         this))}))


(defn lambda
  "
  Make a component that, when started, runs an endless AWS message
  processing loop in a separate thread. The stop action interrupts
  the thread and joins it.
  "
  ([]
   (-> nil
       (map->LambdaHandler)
       (with-component-meta)))

  ([handler]
   (-> {:handler handler}
       (map->LambdaHandler)
       (with-component-meta))))
