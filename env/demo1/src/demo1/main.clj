(ns demo1.main
  (:require
   [lambda.ring :as ring]
   [lambda.main :as main]

   [ring.middleware.json
    :refer [wrap-json-body
            wrap-json-response]]

   [ring.middleware.keyword-params
    :refer [wrap-keyword-params]]

   [ring.middleware.params
    :refer [wrap-params]])

  (:gen-class))


(defn handler [request]

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


(defn -main [& _]
  (main/run fn-event))
