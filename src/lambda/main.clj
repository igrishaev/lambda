;; https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html
;; https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html

(ns lambda.main
  (:require
   [lambda.log :as log]
   [lambda.api :as api]
   [lambda.error :as e])
  (:import
   (java.util.function Function
                       Supplier)
   (java.util.concurrent CompletableFuture)
   (java.util.concurrent Executors)))


(set! *warn-on-reflection* true)


(defmacro supplier [& body]
  `(reify Supplier
     (get [this#]
       ~@body)))


(defmacro function [[bind] & body]
  `(reify Function
     (apply [this# ~bind]
       ~@body)))


(defn run [fn-event]

  (while true
    (let [{:keys [headers body]}
          (api/next-invocation)

          request-id
          (get headers :lambda-runtime-aws-request-id)]

      (-> (CompletableFuture/supplyAsync
           (supplier
             (fn-event body)))
          (.thenApplyAsync
           (function [response]
             (api/invocation-response request-id response)))
          (.exceptionally
           (function [e]
             (log/errorf "Event error, request ID: %s" request-id)
             (log/exception e)
             (api/invocation-error request-id e))))))

  #_
  (let [executor
        (Executors/newFixedThreadPool 4)]


    ))
