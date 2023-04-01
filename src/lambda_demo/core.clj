(ns lambda-demo.core
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.client :as client])
  (:gen-class))


(defmacro logf [template & args]
  `(println (format ~template ~@args)))


(defn -main [& args]

  (let [lambda-host
        (System/getenv "AWS_LAMBDA_RUNTIME_API")

        url-next
        (format "http://%s/2018-06-01/runtime/invocation/next" lambda-host)]

    (while true

      (let [{:keys [status headers body]}
            @(client/get url-next {:as :stream})

            event-data
            (json/parse-stream
             (io/reader body))

            invocation-id
            (get headers :lambda-runtime-aws-request-id)

            url-resp
            (format "http://%s/2018-06-01/runtime/invocation/%s/response"
                    lambda-host invocation-id)

            response
            {:statusCode 200
             :headers {:Content-Type "application/json"}
             :body (json/generate-string {:data event-data})}]

        @(client/post url-resp
                      {:body (json/generate-string response)})))))
