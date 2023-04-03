(ns lambda-demo.core-test
  (:require
   [lambda-demo.ring :as ring]

   [ring.middleware.json
    :refer [wrap-json-body
            wrap-json-response]]

   [ring.middleware.keyword-params
    :refer [wrap-keyword-params]]

   [ring.middleware.params
    :refer [wrap-params]]


   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))


(def capture! (atom nil))


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
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (ring/wrap-ring-event)))


(deftest test-response

  (let [event
        (-> "event.json"
            io/resource
            io/reader
            (json/parse-stream keyword))

        response
        (fn-event event)]

    (is (= {:statusCode 200,
            :headers {"Content-Type" "application/json; charset=utf-8"},
            :isBase64Encoded false,
            :body "{\"aaa\":1}"}
           response))))


(deftest test-request

  (let [event
        (-> "event.json"
            io/resource
            io/reader
            (json/parse-stream keyword))

        response
        (fn-event event)

        request
        @capture!]

    (is (= {:user-agent "agent",
            :protocol "HTTP/1.1",
            :remote-addr "123.123.123.123",
            :params {},
            :headers {"content-type" "application/json",
                      "header2" "value1,value2"},
            :form-params {},
            :query-params {:parameter1 "value1,value2", :parameter2 "value"},
            :uri "/my/path",
            :query-string "parameter1=value1&parameter1=value2&parameter2=value",
            :body {:foo 123},
            :request-method :post}

           request))))
