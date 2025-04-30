(ns lambda.env
  (:require
   [lambda.error :refer [error!]]))

(defn env! ^String [^String env-name]
  (or (System/getenv env-name)
      (error! "Env %s not set" env-name)))

(defn env
  (^String [^String env-name]
   (System/getenv env-name))

  ([^String env-name default]
   (or (System/getenv env-name) default)))

(defn env-long ^Long [env-name default]
  (or (some-> env-name System/getenv parse-long)
      default))
