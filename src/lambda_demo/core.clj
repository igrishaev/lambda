(ns lambda-demo.core
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.client :as client])
  (:gen-class))


(defmacro logf [template & args]
  `(binding [*out* *err*]
     (println (format ~template ~@args))))


(defn -main [& args]

  (while true

    (let [lambda-host
          (System/getenv "AWS_LAMBDA_RUNTIME_API")

          url-next
          (format "http://%s/2018-06-01/runtime/invocation/next" lambda-host)


          {:keys [status headers body]}
          @(client/get url-next {:as :stream})

          _ (logf "headers: %s" headers)

          event-data
          (json/parse-stream
           (io/reader body))

          _ (logf "event-data: %s" event-data)

          invocation-id
          (or (get headers "lambda-runtime-aws-request-id")
              (get headers :lambda-runtime-aws-request-id))

          url-resp
          (format "http://%s/2018-06-01/runtime/invocation/%s/response"
                  lambda-host invocation-id)

          response
          {:statusCode 200
           :headers {:Content-Type "text/plain"}
           :body "hello"}

          {:keys [status body]}
          @(client/post url-resp
                        {:as :stream
                         :body (json/generate-string response)
                         :headers {"content-type" "application/json"}})

          _ (logf "status: %s" status)
          _ (logf "body: %s" (slurp body))]


      )))
