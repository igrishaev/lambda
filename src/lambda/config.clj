(ns lambda.config
  "
  A namespace that serves as a config object.
  Each property is a no-arg function which gets
  cached. Cannot declare these as static variables
  on top of the namespace because GraalVM freezes
  runtime (and the env map as well).
  ")

(defmacro defprop [name & body]
  `(def ~name
     (memoize
      (fn []
        ~@body))))

#_:clj-kondo/ignore
(defprop timeout
  (or (some-> "LAMBDA_RUNTIME_TIMEOUT" System/getenv parse-long)
      (* 15 60 1000)))

#_:clj-kondo/ignore
(defprop version
  (or (some-> "LAMBDA_RUNTIME_VERSION" System/getenv)
      "2018-06-01"))

#_:clj-kondo/ignore
(defprop host
  (System/getenv "AWS_LAMBDA_RUNTIME_API"))
