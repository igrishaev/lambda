;; https://github.com/ring-clojure/ring/blob/master/SPEC

(ns lambda.ring
  (:require
   [lambda.log :as log]
   [lambda.codec :as codec]
   [lambda.error :refer [error!]]
   [clojure.java.io :as io]
   [clojure.string :as str]))


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
        (process-headers headers)

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
    (error! "Cannot coerce %s to response body" this))

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
