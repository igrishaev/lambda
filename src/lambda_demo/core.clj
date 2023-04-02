;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html
;; https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
;; https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html
;; https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html

(ns lambda-demo.core
  (:require
   [lambda-demo.codec :as codec]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.client :as client])
  (:gen-class))


(defmacro logf [template & args]
  `(println (format ~template ~@args)))


(defn event->request [event]

  (let [{:keys [headers
                isBase64Encoded
                queryStringParameters
                requestContext
                body]}
        event

        {:keys [http
                requestId]}
        requestContext

        {:keys [method
                path
                protocol
                sourceIp
                userAgent]}
        http

        stream
        (when body
          (if isBase64Encoded
            (-> body
                codec/str->bytes
                codec/b64-decode
                io/input-stream)
            (-> body
                codec/str->bytes
                io/input-stream)))

        request-method
        (some-> method str/lower-case keyword)

        request
        {:remote-addr sourceIp
         :uri path
         :query-string queryStringParameters
         :request-method request-method
         :protocol protocol
         :user-agent userAgent
         :headers headers
         :body stream}]

    (with-meta request {:event event})))


(defn -main [& args]

  (let [lambda-host
        (System/getenv "AWS_LAMBDA_RUNTIME_API")

        url-next
        (format "http://%s/2018-06-01/runtime/invocation/next" lambda-host)]

    (while true

      (let [{:keys [status headers body]}
            @(client/get url-next {:as :stream})

            event-data
            (json/parse-stream (io/reader body) keyword)

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
