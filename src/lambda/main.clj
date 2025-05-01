;; https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html
;; https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html

(ns lambda.main
  (:require
   [lambda.log :as log]
   [lambda.api :as api]
   [lambda.error :as e]))


(defn run [fn-event]

  (while true

    (let [{:keys [headers body]}
          (api/next-invocation)

          request-id
          (get headers :lambda-runtime-aws-request-id)

          [e response]
          (e/with-safe
            (fn-event body))]

      (if e
        (do
          (log/errorf "Event error, request ID: %s" request-id)
          (log/exception e)
          (api/invocation-error request-id e))
        (api/invocation-response request-id response)))))
