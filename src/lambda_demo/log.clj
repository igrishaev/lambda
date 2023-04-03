(ns lambda-demo.log
  (:require
   [clojure.string :as str]))


(defmacro logf [level template & args]
  (let [nsn (ns-name *ns*)]
    `(println '~nsn
              (-> ~level name str/upper-case)
              (format ~template ~@args))))


(defmacro debugf [template & args]
  `(logf :debug ~template ~@args))


(defmacro infof [template & args]
  `(logf :info ~template ~@args))


(defmacro errorf [template & args]
  `(logf :error ~template ~@args))
