;; https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html
;; https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html

(ns lambda.main
  (:require
   [lambda.log :as log]
   [lambda.api :as api]
   [lambda.error :as e]))

(set! *warn-on-reflection* true)


(defn step [fn-event]
  (let [{:keys [headers body]}
        (api/next-invocation)

        request-id
        (get headers "lambda-runtime-aws-request-id")

        [e response]
        (e/with-safe
          (fn-event body))]

    (if e
      (do
        (log/errorf "Event error, request ID: %s" request-id)
        (log/exception e)
        (api/invocation-error request-id e))
      (api/invocation-response request-id response))))


(defn run
  "
  Run an endless event loop in the current thread.
  "
  [fn-event]
  (while true
    (step fn-event)))


(defn run-thread
  "
  Run an endless event loop in a new thread. Returns
  a Thread instance. To stop the loop, interrupt the
  thread and then join it.
  "
  ^Thread [fn-event]
  (let [thread
        (new Thread
             (fn []
               (while (not (Thread/interrupted))
                 (step fn-event))))]
    (.start thread)
    thread))
