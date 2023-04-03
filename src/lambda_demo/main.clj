(ns lambda-demo.main
  (:require
   [lambda-demo.codec :as codec]
   [lambda-demo.api :as api]
   [lambda-demo.error :as e]
   [lambda-demo.log :as log]))


(defn run

  ([fn-event]
   (run nil fn-event))

  ([fn-init fn-event]

   (let [[e init]
         (e/with-safe
           (when fn-init
             (fn-init)))]

     (when e
       (api/init-error e)
       (log/errorf "Init error: %s" e)
       (e/exit! 1))

     (while true

       (let [{:keys [status headers body]}
             (api/next-invocation)

             request-id
             (get headers :lambda-runtime-aws-request-id)

             [e response]
             (e/with-safe
               (if fn-init
                 (fn-event init body)
                 (fn-event body)))]

         (if e
           (api/invocation-error request-id e)
           (api/invocation-response request-id response)))))))
