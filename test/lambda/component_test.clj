(ns lambda.component-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.stuartsierra.component :as component]
   [lambda.component :as lc]))


(deftest test-component-ok
  (let [c (lc/lambda +)]
    (is (satisfies? component/Lifecycle c))
    (is (= "<LambdaHandler, handler: #function[clojure.core/+], thread: null>"
           (str c)))
    (is (= "<LambdaHandler, handler: #function[clojure.core/+], thread: null>"
           (pr-str c)))))
