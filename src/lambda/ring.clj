;; https://github.com/ring-clojure/ring/blob/master/SPEC

(ns lambda.ring
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [jsam.core :as jsam]
   [lambda.codec :as codec]
   [lambda.error :refer [throw!
                         with-safe]]
   [lambda.log :as log]))


(defn process-headers
  [headers]
  (persistent!
   (reduce-kv
    (fn [acc! k v]
      (let [h (-> k name str/lower-case)]
        (assoc! acc! h v)))
    (transient {})
    headers)))


(defn ->ring [event]

  (let [{:keys [headers
                isBase64Encoded
                rawQueryString
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

        norm-headers
        (-> headers
            (process-headers)
            (assoc "x-request-id" requestId))

        request
        {:remote-addr sourceIp
         :uri path
         :query-params queryStringParameters
         :query-string rawQueryString
         :request-method request-method
         :protocol protocol
         :user-agent userAgent
         :headers norm-headers
         :body stream}]

    (with-meta request {:event event})))


(defprotocol IBody
  (->body [this]))


(extend-protocol IBody

  Object
  (->body [this]
    (throw! "Cannot coerce %s to response body" this))

  String
  (->body [this]
    [false this])

  clojure.lang.ISeq
  (->body [this]
    (->body (with-out-str
              (doseq [line this]
                (print line)))))

  java.io.File
  (->body [this]
    (->body (io/input-stream this)))

  java.io.InputStream
  (->body [this]
    (let [array
          (-> this
              .readAllBytes)]
      [true
       (-> array
           codec/b64-encode
           codec/bytes->str)])))


(defn ring-> [response]

  (let [{:keys [status
                headers
                body]}
        response

        [b64-encoded? string]
        (->body body)]

    {:statusCode status
     :headers headers
     :isBase64Encoded b64-encoded?
     :body string}))


(defn wrap-ring-event [handler]
  (fn [event]
    (-> event
        (->ring)
        (handler)
        (ring->))))


(def response-internal-error
  {:status 500
   :headers {"content-type" "text/plain"}
   :body "Internal server error"})


;; TODO: docstring
(defn wrap-ring-exeption [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (let [{:keys [uri
                      request-method]}
              request]
          (log/errorf "Unhandled exception in a Ring handler, method: %s, uri: %s"
                      request-method uri)
          (log/exception e)
          response-internal-error)))))


;;
;; JSON middleware
;;

(defn json-request? [request]
  (when-let [content-type
             (get-in request [:headers "content-type"])]
    (re-find #"^(?i)application/(.+\+)?json" content-type)))


(def response-json-malformed
  {:status 400
   :headers {"content-type" "text/plain"}
   :body "Malformed JSON payload"})


(defn wrap-json-body
  [handler]
  (fn [request]
    (if (json-request? request)
      (let [[e request-json]
            (with-safe
              (update request :body jsam/read))]
        (if e
          response-json-malformed
          (handler request-json)))
      (handler request))))


(defn assoc-json-params [request json]
  (if (map? json)
    (-> request
        (assoc :json-params json)
        (update-in [:params] merge json))
    request))


(defn wrap-json-params [handler]
  (fn [request]
    (if (json-request? request)
      (let [[e data]
            (with-safe
              (some-> request :body jsam/read))]
        (if e
          response-json-malformed
          (-> request
              (assoc-json-params data)
              (handler))))
      (handler request))))


;; TODO: pass options

(def CONTENT-TYPE-JSON
  "application/json; charset=utf-8")

(defn wrap-json-response
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (-> response :body coll?)
        (-> response
            (update :body jsam/write-string)
            (assoc-in [:headers "content-type"] CONTENT-TYPE-JSON))
        response))))
