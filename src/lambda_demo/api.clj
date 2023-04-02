;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html

(ns lambda-demo.api
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [org.httpkit.client :as client]))


(defn api-call
  ([method path]
   (api-call method path nil))

  ([method path data]

   (let [host
         (System/getenv "AWS_LAMBDA_RUNTIME_API")

         url
         (format "http://%s/2018-06-01" host path)

         {:keys [status body]}
         @(client/request url {:method method
                               :body (when data
                                       (json/generate-string data))})

         {:keys [StatusResponse
                 ErrorResponse]}
         (when body
           (json/parse-stream (io/reader body) keyword))]

     (case (long status)

       (200 201 202)
       StatusResponse

       (400 403)
       ErrorResponse

       (500)
       (do
         ;; log everything
         (System/exit 1))

       ;; else
       42))))


(defn next-invocation []
  (api-call :get "/runtime/invocation/next"))


(defn invocation-response [^String aws-request-id data]
  (let [path
        (format "/runtime/invocation/%s/response" aws-request-id)]
    (api-call :post path data)))
