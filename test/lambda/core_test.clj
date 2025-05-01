(ns lambda.core-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [jsam.core :as jsam]
   [lambda.ring :as ring]
   [ring.middleware.keyword-params
    :refer [wrap-keyword-params]]
   [ring.middleware.params
    :refer [wrap-params]]))


(def capture! (atom nil))


#_:clj-kondo/ignore
(defn handler [request]

  (reset! capture! request)

  (let [{:keys [request-method
                uri
                headers
                body]}
        request]

    {:status 200
     :body {:aaa 1}}))



(def fn-event
  (-> handler
      (wrap-keyword-params)
      (wrap-params)
      (ring/wrap-json-body)
      (ring/wrap-json-response)
      (ring/wrap-ring-event)))


(deftest test-response

  (let [event
        (-> "event.json"
            (io/resource)
            (jsam/read))

        response
        (fn-event event)]

    (is (= {:statusCode 200,
            :headers {"content-type" "application/json; charset=utf-8"},
            :isBase64Encoded false,
            :body "{\"aaa\":1}"}
           response))))


#_:clj-kondo/ignore
(deftest test-request

  (let [event
        (-> "event.json"
            (io/resource)
            (jsam/read))

        response
        (fn-event event)

        request
        @capture!]

    (is (= {:user-agent "agent",
            :protocol "HTTP/1.1",
            :remote-addr "123.123.123.123",
            :params {},
            :headers {"content-type" "application/json",
                      "header2" "value1,value2"
                      "x-request-id" "id"},
            :form-params {},
            :query-params {:parameter1 "value1,value2", :parameter2 "value"},
            :uri "/my/path",
            :query-string "parameter1=value1&parameter1=value2&parameter2=value",
            :body {:foo 123}
            :request-method :post}

           request))))


#_:clj-kondo/ignore
(deftest test-form-params

  (let [event
        (-> "event.json"
            (io/resource)
            (jsam/read)
            (assoc-in [:headers "Content-Type"]
                      "application/x-www-form-urlencoded")
            (assoc-in [:body]
                      "test=foo&hello=bar"))

        response
        (fn-event event)

        request
        @capture!]

    (is (= {:user-agent "agent",
            :protocol "HTTP/1.1",
            :remote-addr "123.123.123.123",
            :params {:test "foo", :hello "bar"},
            :headers
            {"content-type" "application/x-www-form-urlencoded",
             "header2" "value1,value2"
             "x-request-id" "id"},
            :form-params {"test" "foo", "hello" "bar"},
            :query-params {:parameter1 "value1,value2", :parameter2 "value"},
            :uri "/my/path",
            :query-string "parameter1=value1&parameter1=value2&parameter2=value",
            :request-method :post}

           (dissoc request :body)))))
