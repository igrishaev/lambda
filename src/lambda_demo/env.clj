(ns lambda-demo.env
  (:require
   [lambda-demo.error :refer [error!]]))


(defn env! [^String env-name]
  (or (System/getenv env-name)
      (error! "Env %s not set" env-name)))
