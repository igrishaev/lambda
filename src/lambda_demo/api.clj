;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html

(ns lambda-demo.api
  (:require
   [lambda-demo.log :as log]
   [lambda-demo.error :refer [error!]]
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [org.httpkit.client :as client]))


(defn parse-response [response]
  (update response
          :body
          (fn [body]
            (when body
              (json/parse-stream (io/reader body) keyword)))))


(defn api-call
  ([method path]
   (api-call method path nil))

  ([method path data]

   (let [host
         (System/getenv "AWS_LAMBDA_RUNTIME_API")

         url
         (format "http://%s/2018-06-01" host path)

         params
         {:method method
          :body (when data
                  (json/generate-string data))}

         {:as response :keys [status body]}
         @(client/request url params parse-response)]

     (case (long status)

       (200 201 202)
       response

       (400 403)
       (error! "Got 4xx response from the Runtime API, method: %s, url: %s, status: %s, body: %s"
               method url status body)

       (500)
       (do
         (log/errorf "Got 5xx response from the Runtime API, method: %s, url: %s, status: %s, body: %s"
                     method url status body)
         (System/exit 1))

       ;; else
       (do
         (log/errorf "Got unknown response from the Runtime API, method: %s, url: %s, status: %s, body: %s"
                     method url status body)
         (System/exit 1))))))


(defn next-invocation []
  (api-call :get "/runtime/invocation/next"))


(defn invocation-response [^String aws-request-id data]
  (let [path
        (format "/runtime/invocation/%s/response" aws-request-id)]
    (api-call :post path data)))


(defn e->err-payload [e]
  {:errorMessage "bb"
   :errorType "aa"
   :stackTrace ["a" "b" "c"]})


(defn init-error
  ([e]
   (init-error e nil))

  ([^Throwable e ^String error-type]

   ;; TODO: error-type header
   ;; Lambda-Runtime-Function-Error-Type
   ;; Runtime.NoSuchHandler
   ;; Runtime.APIKeyNotFound
   ;; Runtime.ConfigInvalid
   ;; Runtime.UnknownReason
   (let [path
         "/runtime/init/error"

         data
         (e->err-payload e)]

     (api-call :post path data))))


(defn invocation-error
  ([request-id e]
   (invocation-error request-id e nil))

  ([^String request-id ^Throwable e ^String error-type]

   ;; TODO: error-type header
   ;; Lambda-Runtime-Function-Error-Type
   ;; Runtime.NoSuchHandler
   ;; Runtime.APIKeyNotFound
   ;; Runtime.ConfigInvalid
   ;; Runtime.UnknownReason
   (let [path
         (format "/runtime/invocation/request-id/error" request-id)

         data
         (e->err-payload e)]

     (api-call :post path data))))
