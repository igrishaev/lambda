(ns lambda-demo.log
  (:require
   [clojure.string :as str]))


(defmacro logf [level template & args]
  `(println (ns-name *ns*)
            (-> ~level name str/upper-case)
            (format ~template ~@args)))
