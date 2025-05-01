(ns lambda.json-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [is deftest]]
   [lambda.ring :as ring]))


(deftest test-wrap-json-response

  (let [handler
        (ring/wrap-json-response
         (fn [_request]
           {:status 200
            :body {:foo 123}
            :headers {"hello" "test"}}))]

    (is (= {:status 200,
            :body "{\"foo\":123}",
            :headers
            {"hello" "test",
             "content-type" "application/json; charset=utf-8"}}
           (handler {:method :get}))))

  (let [handler
        (ring/wrap-json-response
         (fn [_request]
           {:status 200
            :body "dunno"
            :headers {"hello" "test"}}))]

    (is (= {:status 200
            :body "dunno"
            :headers {"hello" "test"}}
           (handler {:method :get})))))


(defn ->stream [^String string]
  (-> string
      .getBytes
      io/input-stream))


(deftest test-wrap-json-body

  (let [handler
        (ring/wrap-json-body
         (fn [request] request))]
    (is (= {:body "[1, 2, 3]"}
           (handler {:body "[1, 2, 3]"}))))

  (let [handler
        (ring/wrap-json-body
         (fn [request] request))]
    (is (= {:body [1 2 3]
            :headers {"content-type" "application/json"}}
           (handler {:body (->stream "[1, 2, 3]")
                     :headers {"content-type" "application/json"}}))))

  (let [handler
        (ring/wrap-json-body
         (fn [request] request))]
    (is (= {:status 400
            :headers {"content-type" "text/plain"}
            :body "Malformed JSON payload"}
           (handler {:body (->stream "dunno-lol")
                     :headers {"content-type" "application/json"}})))))


(deftest test-wrap-json-params

  (let [handler
        (ring/wrap-json-params
         (fn [request] request))]
    (is (= {:body "[1, 2, 3]"}
           (handler {:body "[1, 2, 3]"}))))

  (let [handler
        (ring/wrap-json-params
         (fn [request] request))]
    (is (= {:headers {"content-type" "application/json"}}
           (-> {:body (->stream "[1, 2, 3]")
                :headers {"content-type" "application/json"}}
               (handler)
               (dissoc :body)))))

  (let [handler
        (ring/wrap-json-params
         (fn [request] request))]
    (is (= {:headers {"content-type" "application/json"}
            :json-params {:foo 123}
            :params {:foo 123}}
           (-> {:body (->stream "{\"foo\": 123}")
                :headers {"content-type" "application/json"}}
               (handler)
               (dissoc :body)))))

  (let [handler
        (ring/wrap-json-params
         (fn [request] request))]
    (is (= {:params {:lol "bar", :foo 123}
            :headers {"content-type" "application/json"}
            :json-params {:foo 123}}
           (-> {:body (->stream "{\"foo\": 123}")
                :params {:lol "bar"}
                :headers {"content-type" "application/json"}}
               (handler)
               (dissoc :body)))))

  (let [handler
        (ring/wrap-json-params
         (fn [request] request))]
    (is (= {:status 400
            :headers {"content-type" "text/plain"}
            :body "Malformed JSON payload"}
           (handler {:body (->stream "dunno-lol")
                     :headers {"content-type" "Application/Json"}})))))
