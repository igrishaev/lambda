;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html

(ns lambda.api
  (:require
   [babashka.http-client :as http]
   [clojure.string :as str]
   [jsam.core :as jsam]
   [lambda.config :as config]
   [lambda.error :as e]))

(defn parse-response [response]
  (update response
          :body
          (fn [body]
            (some-> body jsam/read))))


(defn api-call
  ([method path]
   (api-call method path nil))

  ([method path data]
   (api-call method path data nil))

  ([method path data headers]

   (let [host
         #_:clj-kondo/ignore (config/host)

         url
         (format "http://%s/%s%s"
                 host
                 #_:clj-kondo/ignore (config/version)
                 path)

         request
         {:url url
          :method method
          :headers headers
          :as :stream
          :timeout #_:clj-kondo/ignore (config/timeout)
          :body (when data
                  (jsam/write-string data))}]

     (try
       (http/request request)
       (catch Exception e
         (e/throw! "Failed to interact with the Runtime API, method: %s, url: %s, status: %s, message: %s"
                   method
                   url
                   (some-> e ex-data :status)
                   (ex-message e)))))))


(defn next-invocation []
  (api-call :get "/runtime/invocation/next"))


(defn invocation-response [^String request-id data]
  (let [path
        (format "/runtime/invocation/%s/response" request-id)]
    (api-call :post path data)))


(defn ex->ErrorRequest [e]

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
  [e]
  (let [path
        "/runtime/init/error"

        data
        (ex->ErrorRequest e)

        headers
        {"Lambda-Runtime-Function-Error-Type"
         "Runtime.UnknownReason"}]

    (api-call :post path data headers)))


(defn invocation-error
  [request-id e]
  (let [path
        (format "/runtime/invocation/%s/error" request-id)

        data
        (ex->ErrorRequest e)

        headers
        {"Lambda-Runtime-Function-Error-Type"
         "Runtime.UnknownReason"}]

    (api-call :post path data headers)))
