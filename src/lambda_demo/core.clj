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
     :headers {"content-type" "text/plain"}
     :body (str request-method
                \space
                uri
                \space
                headers
                \space
                body)}))


#_
(defn fn-init []
  {:foo 1
   :bar 2
   :kek 3})


#_
(defn fn-event
  [init event]
  {:statusCode 200
   :headers {:Content-Type "text/plain"}
   :body "it works!"})


(def fn-event
  (-> handler
      ring/wrap-ring-event))


(defn -main [& _]
  (main/run #_fn-init fn-event))
