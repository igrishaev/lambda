(ns lambda.gzip-test
  (:import
   (java.io File))
  (:require
   [lambda.config :as config]
   [lambda.codec :as codec]
   [clojure.java.io :as io]
   [clojure.test :refer [is deftest testing]]
   [lambda.ring :as ring]))


(deftest test-codec
  (is (= [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
         (-> "test"
             (codec/str->bytes)
             (codec/bytes->gzip)
             (vec))))
  (is (= "test"
         (-> [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
             byte-array
             (codec/gzip->bytes)
             (codec/bytes->str)))))


(deftest test-wrap-gzip-encode-response

  (testing "no req/resp header"
    (let [handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body "test"}))]
      (is (= {:status 200 :body "test"}
             (handler {})))))

  (testing "override config"
    (let [handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body "test"}))]
      (is (= {:status 200
              :body [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
              :headers {"content-encoding" "gzip"}}
             #_:clj-kondo/ignore
             (with-redefs [config/gzip? (constantly true)]
               (-> {}
                   (handler)
                   (update :body vec)))))))

  (testing "request header, string"
    (let [handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body "test"}))]
      (is (= (-> {:status 200
                  :body [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
                  :headers {"content-encoding" "gzip"}}
                 (update :body vec))
             (-> {:headers {"accept-encoding" "gzip"}}
                 (handler)
                 (update :body vec))))))

  (testing "request header, byte array"
    (let [handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body (byte-array [116, 101, 115, 116])}))]
      (is (= (-> {:status 200
                  :body [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
                  :headers {"content-encoding" "gzip"}}
                 (update :body vec))
             (-> {:headers {"accept-encoding" "gzip"}}
                 (handler)
                 (update :body vec))))))

  (testing "request header, nil"
    (let [handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body nil}))]
      (is (= (-> {:status 200
                  :body nil
                  :headers {"content-encoding" "gzip"}})
             (-> {:headers {"accept-encoding" "gzip"}}
                 (handler))))))

  (testing "request header, input-stream"
    (let [handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body (io/input-stream (byte-array [116, 101, 115, 116]))}))]
      (is (= (-> {:status 200
                  :body [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
                  :headers {"content-encoding" "gzip"}}
                 (update :body vec))
             (-> {:headers {"accept-encoding" "gzip"}}
                 (handler)
                 (update :body vec))))))

  (testing "request header, file"
    (let [file
          (File/createTempFile "test" ".txt")

          _ (spit file "test")

          handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body file}))]
      (is (= (-> {:status 200
                  :body [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
                  :headers {"content-encoding" "gzip"}}
                 (update :body vec))
             (-> {:headers {"accept-encoding" "gzip"}}
                 (handler)
                 (update :body vec))))))

  (testing "request header, coll of strings"
    (let [handler
          (ring/wrap-gzip
           (fn [_request]
             {:status 200
              :body (seq "test")}))]
      (is (= (-> {:status 200
                  :body [31 -117 8 0 0 0 0 0 0 -1 43 73 45 46 1 0 12 126 127 -40 4 0 0 0]
                  :headers {"content-encoding" "gzip"}}
                 (update :body vec))
             (-> {:headers {"accept-encoding" "gzip"}}
                 (handler)
                 (update :body vec)))))))


(deftest test-wrap-gzip-decode-request

  (testing "body is not gzip-encoded"
    (let [handler
          (ring/wrap-gzip
           (fn [request]
             {:status 200
              :body (format "body: %s" (-> request :body slurp))}))]
      (is (= {:status 200
              :body "body: test"}
             (-> {:body (io/input-stream (.getBytes "test"))}
                 (handler))))))

  (testing "body is not gzip-encoded"
    (let [handler
          (ring/wrap-gzip
           (fn [request]
             {:status 200
              :body (format "body: %s" (-> request :body slurp))}))]
      (is (= {:status 200
              :body "body: test"}
             (-> {:body (io/input-stream (codec/bytes->gzip (.getBytes "test")))
                  :headers {"content-encoding" "gzip"}}
                 (handler)))))))
