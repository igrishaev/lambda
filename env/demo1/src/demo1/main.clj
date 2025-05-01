(ns demo1.main
  (:gen-class)
  (:require
   [lambda.main :as main]
   [lambda.ring :as ring]
   [ring.middleware.keyword-params
    :refer [wrap-keyword-params]]
   [ring.middleware.params
    :refer [wrap-params]]))


(defn handler [request]

  (let [{:keys [request-method
                uri
                headers
                body]}
        request]

    {:status 200
     :body {:bbb 1}}))


(def fn-event
  (-> handler
      (wrap-keyword-params)
      (wrap-params)
      (ring/wrap-json-body) ;; TODO params
      (ring/wrap-json-response)
      (ring/wrap-ring-event)))


(defn -main [& _]
  (main/run fn-event))
