(ns demo2.main
  (:require
   [lambda.log :as log]
   [lambda.main :as main])
  (:gen-class))


(defn process-event [db event]
  (jdbc/with-transaction [tx db]
    (jdbc/insert! tx ...)
    (jdbc/delete! tx ...)))


(defn make-handler []

  (let [config
        (-> "config.edn"
            io/resource
            aero/read-config)

        db
        (jdbc/get-connection (:db config))]

    (fn [event]
      (process-event db event))))


(defn -main [& _]
  (let [handler (make-handler)]
    (main/run handler)))
