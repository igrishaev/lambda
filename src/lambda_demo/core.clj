(ns lambda-demo.core
  (:require
   [lambda-demo.ring :as ring]
   [lambda-demo.main :as main]
   [lambda-demo.log :as log])
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
      ;; (wrap-keyword-params)
      ;; (wrap-params)
      ;; (wrap-json-body {:keywords? true})
      ;; (wrap-json-response)
      (ring/wrap-ring-event)))


(defn -main [& _]
  (main/run fn-event))
