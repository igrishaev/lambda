(ns lambda-demo.error)


(defmacro error! [template & args]
  `(throw (new Exception (format ~template ~@args))))


(defn exit!
  ([]
   (exit! 0))
  ([code]
   (System/exit code)))


(defmacro with-safe [& body]
  `(try
     (let [result# (do ~@body)]
       [nil result#])
     (catch Throwable e#
       [e# nil])))
