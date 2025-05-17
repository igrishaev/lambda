;; https://github.com/ring-clojure/ring/blob/master/SPEC

(ns lambda.ring
  "
  A namespace to mimic Ring functionality, namely:
  - turn Lambda HTTP events into Ring maps and back;
  - provide custom Ring middleware.
  "
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [jsam.core :as jsam]
   [lambda.codec :as codec]
   [lambda.config :as config]
   [lambda.error :refer [throw!
                         with-safe]]
   [lambda.log :as log])
  (:import
   (clojure.lang ISeq)
   (java.io File
            InputStream)
   (java.util.zip GZIPInputStream)))


(defn process-headers
  "
  Turn Lambda headers into a Ring headers map.
  "
  [headers]
  (persistent!
   (reduce-kv
    (fn [acc! k v]
      (let [h (-> k name str/lower-case)]
        (assoc! acc! h v)))
    (transient {})
    headers)))


(defn ->ring
  "
  Turn Lambda event into a Ring map.
  "
  [event]

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


(def TYPE_ARR_BYTE
  (Class/forName "[B"))


;; A protocol to coerse various Ring responses
;; to a Lambda response. Must return a couple of
;; [is-base64-encoded?, string].
(defprotocol IBody
  (->body [this]))


(extend-protocol IBody

  nil
  (->body [_]
    [false ""])

  Object
  (->body [this]
    (throw! "Cannot coerce %s to response body" this))

  String
  (->body [this]
    [false this])

  ISeq
  (->body [this]
    (->body (apply str this)))

  File
  (->body [this]
    (->body (io/input-stream this)))

  InputStream
  (->body [this]
    (->body (codec/read-bytes this))))


;; Arrays can be extended like this only
(extend TYPE_ARR_BYTE
  IBody
  {:->body
   (fn [this]
     [true (-> this
               codec/b64-encode
               codec/bytes->str)])})


(defn ring->
  "
  Turn a Ring response map into a Lambda HTTP response.
  "
  [response]

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


(defn wrap-ring-event
  "
  A ring middleware that transforms an HTTP Lambda
  event into a Ring request, processes it with a
  Ring handler, and turns the result into a Lambda
  HTTP response.
  "
  [handler]
  (fn [event]
    (-> event
        (->ring)
        (handler)
        (ring->))))


(def response-internal-error
  {:status 500
   :headers {"content-type" "text/plain"}
   :body "Internal server error"})


(defn wrap-ring-exception
  "
  A middleware what captures any Ring exceptions,
  logs them and returns a negative HTTP response.
  "
  [handler]
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

(defn json-request?
  "
  Check if the Ring request was of a JSON type.
  "
  [request]
  (when-let [content-type
             (get-in request [:headers "content-type"])]
    (re-find #"^(?i)application/(.+\+)?json" content-type)))


(def response-json-malformed
  {:status 400
   :headers {"content-type" "text/plain"}
   :body "Malformed JSON payload"})


(defn wrap-json-body
  "
  A middleware that, if the request was JSON,
  replaces the :body field with the parsed JSON
  data. Takes an optional map of Jsam settings.
  "
  ([handler]
   (wrap-json-body handler nil))

  ([handler opt]
   (fn [request]
     (if (json-request? request)
       (let [[e request-json]
             (with-safe
               (update request :body jsam/read opt))]
         (if e
           response-json-malformed
           (handler request-json)))
       (handler request)))))


(defn assoc-json-params [request json]
  (if (map? json)
    (-> request
        (assoc :json-params json)
        (update-in [:params] merge json))
    request))


(defn wrap-json-params
  "
  A middleware that, if the request was JSON,
  adds the :json-params field to the request,
  and also merged then with :params, if the
  data was a map. Takes an optional map of
  Jsam settings.
  "
  ([handler]
   (wrap-json-params handler nil))

  ([handler opt]
   (fn [request]
     (if (json-request? request)
       (let [[e data]
             (with-safe
               (some-> request :body (jsam/read opt)))]
         (if e
           response-json-malformed
           (-> request
               (assoc-json-params data)
               (handler))))
       (handler request)))))


(def CONTENT-TYPE-JSON
  "application/json; charset=utf-8")


(defn wrap-json-response
  "
  A middleware that, if the body of the response
  was a collection, transforms the body into
  a JSON string and adds a Content-Type header
  with JSON mime-type. Takes an optional map of
  Jsam settings.
  "
  ([handler]
   (wrap-json-response handler nil))

  ([handler opt]
   (fn [request]
     (let [response (handler request)]
       (if (-> response :body coll?)
         (-> response
             (update :body jsam/write-string opt)
             (assoc-in [:headers "content-type"] CONTENT-TYPE-JSON))
         response)))))


;;
;; GZip middleware
;;

(defn accept-gzip? [request]
  (or #_:clj-kondo/ignore (config/gzip?)
      (some-> request
              :headers
              (get "accept-encoding")
              (str/includes? "gzip"))))


(defn encoded-gzip? [request]
  (some-> request
          :headers
          (get "content-encoding")
          (str/includes? "gzip")))


(defprotocol IGzip
  (-gzip-encode [this]))


(extend-protocol IGzip

  nil
  (-gzip-encode [_]
    nil)

  Object
  (-gzip-encode [this]
    (throw! "Cannot gzip-encode body: %s" this))

  String
  (-gzip-encode [this]
    (-> this
        codec/str->bytes
        codec/bytes->gzip))

  ISeq
  (-gzip-encode [this]
    (-gzip-encode (apply str this)))

  File
  (-gzip-encode [this]
    (-gzip-encode (io/input-stream this)))

  InputStream
  (-gzip-encode [this]
    (-> this
        codec/read-bytes
        codec/bytes->gzip)))


(extend TYPE_ARR_BYTE
  IGzip
  {:-gzip-encode
   (fn [this]
     (codec/bytes->gzip this))})


(defn gzip-response
  "
  Gzip-encode a Ring response in two steps:
  - assoc a header;
  - encode the body payload.
  "
  [response]
  (-> response
      (assoc-in [:headers "content-encoding"] "gzip")
      (update :body -gzip-encode)))


(defn ungzip-request
  "
  Wrap the request's :body field with a class
  that decodes gzip payload on the fly.
  "
  [request]
  (update request
          :body
          (fn [input-stream]
            (new GZIPInputStream input-stream))))


(defn wrap-gzip
  "
  Wrap a given handler with in/out gzip logic.

  If a client sends a header Content-Encoding: gzip,
  then the :body of the request is wrapped into an
  instance of GzipInputStream.

  If a client sends a header Accept-Encoding: gzip,
  then the :body of the response is encoded into
  a Gzipped byte array. In addition, the response
  gets a header Content-Encoding: gzip. Supported
  body types are: nil, String, native byte-array,
  ISeq(of String), and InputStream.

  Gzip output encoding might be forced with a global
  configuration (see the lambda.config namespace).
  "
  [handler]
  (fn [request]

    (let [encoded?
          (encoded-gzip? request)

          accept?
          (accept-gzip? request)]

      (cond-> request

        encoded?
        (ungzip-request)

        :then
        (handler)

        accept?
        (gzip-response)))))
