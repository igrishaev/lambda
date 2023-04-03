(ns lambda-demo.core
  (:require
   [lambda-demo.main :as main]
   [lambda-demo.log :as log])
  (:gen-class))


(defn fn-init []
  {:foo 1
   :bar 2
   :kek 3})


(defn fn-event
  [init event]
  {:statusCode 200
   :headers {:Content-Type "text/plain"}
   :body "it works!"})


(defn -main [& _]
  (main/run fn-init fn-event))
