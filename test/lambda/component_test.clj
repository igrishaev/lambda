(ns lambda.component-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [com.stuartsierra.component :as component]
   [lambda.component :as lc]))


(deftest test-component-ok
  (let [c (lc/lambda +)]
    (is (satisfies? component/Lifecycle c))
    (is (-> c
            str
            (str/starts-with? "<LambdaHandler, handler")))
    (is (-> c
            pr-str
            (str/starts-with? "<LambdaHandler, handler")))))
