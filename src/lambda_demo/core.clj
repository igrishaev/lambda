;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html
;; https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html
;; https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html

(ns lambda-demo.core
  (:require
   [lambda-demo.main :as main]
   [lambda-demo.log :as log])
  (:gen-class))



(defn fn-event [event]
  (log/infof "event: %s" event)
  {:statusCode 200
   :headers {:Content-Type "text/plain"}
   :body "it works!"})


(defn -main [& _]
  (main/run fn-event))
