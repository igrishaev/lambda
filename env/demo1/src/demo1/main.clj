(ns demo1.main
  (:gen-class)
  (:require
   [lambda.main :as main]
   [lambda.ring :as ring]))


(defn handler [request]

  (let [{:keys [request-method
                uri
                headers
                body]}
        request]

    {:status 200
     :body {:request request}}))


(def fn-event
  (-> handler
      (ring/wrap-json-body)
      (ring/wrap-json-response)
      (ring/wrap-gzip)
      (ring/wrap-ring-exception)
      (ring/wrap-ring-event)))


(defn -main [& _]
  (main/run fn-event))
