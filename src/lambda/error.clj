(ns lambda.error)

(defmacro throw! [template & args]
  `(throw (new RuntimeException (format ~template ~@args))))

(defmacro rethrow! [e template & args]
  `(throw (new RuntimeException
               (format ~template ~@args)
               ~e)))

(defmacro with-safe [& body]
  `(try
     (let [result# (do ~@body)]
       [nil result#])
     (catch Throwable e#
       [e# nil])))
