(ns lambda-demo.error)


(defmacro error! [template & args]
  `(throw (new Exception (format ~template ~@args))))
