(ns demo2.main
  (:require
   [lambda.log :as log]
   [lambda.main :as main])
  (:gen-class))


(defn handler [event]
  (log/infof "Event is: %s" event)
  #_(process-event ...)
  {:result [42]})


(defn -main [& _]
  (main/run handler))
