;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html

(ns lambda.api
  (:require
   [lambda.error :as e]
   [lambda.env :as env]
   [clojure.string :as str]
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
   (api-call method path data nil))

  ([method path data headers]

   (let [host
         (env/env! "AWS_LAMBDA_RUNTIME_API")

         url
         (format "http://%s/2018-06-01%s" host path)

         params
         {:as :stream
          :url url
          :headers headers
          :method method
          :timeout Integer/MAX_VALUE
          :body (when data
                  (json/generate-string data))}

         {:as response :keys [status body]}
         @(client/request params parse-response)]

     (case (long status)

       (200 201 202)
       response

       ;; else
       (e/error! "Got negative response from the Runtime API, method: %s, url: %s, status: %s, body: %s"
                 method url status body)))))


(defn next-invocation []
  (api-call :get "/runtime/invocation/next"))


(defn invocation-response [^String request-id data]
  (let [path
        (format "/runtime/invocation/%s/response" request-id)]
    (api-call :post path data)))


(defn e->payload [e]

  (let [{:keys [via
                trace]}
        (Throwable->map e)

        stackTrace
        (for [el trace]
          (str/join \space el))

        errorType
        (-> via first :type)

        errorMessage
        (-> via first :message)]

    {:errorMessage errorMessage
     :errorType errorType
     :stackTrace stackTrace}))


(defn add-error-type [headers ^String error-type]
  (cond-> headers
    error-type
    (assoc "Lambda-Runtime-Function-Error-Type" error-type)))


(defn init-error
  ([e]
   (init-error e nil))

  ([^Throwable e ^String error-type]
   (let [path
         "/runtime/init/error"

         data
         (e->payload e)

         headers
         (add-error-type nil error-type)]

     (api-call :post path data headers))))


(defn invocation-error
  ([request-id e]
   (invocation-error request-id e nil))

  ([^String request-id ^Throwable e ^String error-type]
   (let [path
         (format "/runtime/invocation/%s/error" request-id)

         data
         (e->payload e)

         headers
         (add-error-type nil error-type)]

     (api-call :post path data headers))))
