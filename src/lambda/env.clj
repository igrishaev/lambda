(ns lambda.env
  (:require
   [lambda.error :refer [error!]]))


(defn env! [^String env-name]
  (or (System/getenv env-name)
      (error! "Env %s not set" env-name)))
