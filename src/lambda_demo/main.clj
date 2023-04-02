(ns lambda-demo.main
  (:require
   [lambda-demo.codec :as codec]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.client :as client])
  (:gen-class))


(defn main [event-handler]

  (fn [& _]

    (let [lambda-host
          (System/getenv "AWS_LAMBDA_RUNTIME_API")

          url-next
          (format "http://%s/2018-06-01/runtime/invocation/next" lambda-host)]

      (while true

        (let [{:keys [status headers body]}
              @(client/get url-next {:as :stream})

              event
              (json/parse-stream (io/reader body) keyword)

              invocation-id
              (get headers :lambda-runtime-aws-request-id)

              url-resp
              (format "http://%s/2018-06-01/runtime/invocation/%s/response"
                      lambda-host invocation-id)

              [e response]
              (try
                [nil (event-handler event)]
                (catch Throwable e
                  [e nil]))]

          (if e
            1
            @(client/post url-resp {:body (json/generate-string response)})))))))
