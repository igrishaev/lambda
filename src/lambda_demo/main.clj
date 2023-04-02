(ns lambda-demo.main
  (:require
   [lambda-demo.codec :as codec]
   [lambda-demo.api :as api]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.client :as client])
  (:gen-class))



#_
(defn make-handler []
  (let [config {:foo 1}]
    (fn [event]
      (process-event config event))))


(defn run-loop

  [fn-event]

  (while true

    (let [{:keys [status headers body]}
          (api/next-invocation)

          request-id
          (get headers :lambda-runtime-aws-request-id)

          [e response]
          (try
            [nil (fn-event body)]
            (catch Throwable e
              [e nil]))]

      (if e
        (api/invocation-error request-id e)
        (api/invocation-response request-id response)))))
