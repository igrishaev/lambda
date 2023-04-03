;; https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html
;; https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html

(ns lambda-demo.main
  (:require
   [lambda-demo.log :as log]
   [lambda-demo.api :as api]
   [lambda-demo.error :as e]))


(defn run [fn-event]

  (while true

    (let [{:keys [status headers body]}
          (api/next-invocation)

          request-id
          (get headers :lambda-runtime-aws-request-id)

          [e response]
          (e/with-safe
            (fn-event body))]

      (if e
        (do
          (log/errorf "Event error, request ID: %s, exception: %s"
                      request-id e)
          (api/invocation-error request-id e))
        (api/invocation-response request-id response)))))
