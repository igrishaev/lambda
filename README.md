# Lambda

A small framework to run AWS Lambdas compiled with Native Image.

## Table of Contents

<!-- toc -->

- [Motivation & Benefits](#motivation--benefits)
- [Installation](#installation)
- [Writing Your Lambda](#writing-your-lambda)
  * [Prepare The Code](#prepare-the-code)
  * [Compile It](#compile-it)
    + [Linux (Local Build)](#linux-local-build)
    + [On MacOS (Docker)](#on-macos-docker)
  * [Create a Lambda in AWS](#create-a-lambda-in-aws)
  * [Deploy and Test It](#deploy-and-test-it)
- [Ring Handler for HTTP Requests](#ring-handler-for-http-requests)
- [Sharing the State Between Events](#sharing-the-state-between-events)

<!-- tocstop -->

## Motivation & Benefits

[search]: https://clojars.org/search?q=lambda

There are a lot of Lambda Clojure libraries so far: a [quick search][search] on
Clojars gives several screens of them. What is the point of making a new one?
Well, because none of the existing libraries covers my requirements, namely:

- I want a framework free from any Java SDK, but pure Clojure only.
- I want it to compile into a single binary file so no environment is needed.
- The deployment process must be extremely simple.

As the result, *this* framework:

- Depends only on Http Kit and Cheshire to interact with AWS;
- Provides an endless loop that consumes events from AWS and handles them. You
  only submit a function that processes an event.
- Provides a Ring middleware that turns HTTP events into a Ring handler. Thus,
  you can easily serve HTTP requests with Ring stack.
- Has a built-in logging facility.
- Provides a bunch of Make commands to build a zipped bootstrap file.

## Installation

Leiningen/Boot

```
[com.github.igrishaev/lambda "0.1.1"]
```

Clojure CLI/deps.edn

```
com.github.igrishaev/lambda {:mvn/version "0.1.1"}
```

## Writing Your Lambda

### Prepare The Code

Create a core module with the following code:

```clojure
(ns demo.core
  (:require
   [lambda.log :as log]
   [lambda.main :as main])
  (:gen-class))

(defn handler [event]
  (log/infof "Event is: %s" event)
  (process-event ...)
  {:result [42]})

(defn -main [& _]
  (main/run handler))
```

The `handler` function takes a single argument which is a parsed Lambda
payload. The `lambda.log` namespace provides `debugf`, `infof`, and `errorf`
macros for logging. In the `-main` function you start an endless cycle by
calling the `run` function.

On each step of this cycle, the framework fetches a new event, processes it with
the passed handler and submits the result to AWS. Should the handler fail, it
catches and exception and reports it as well without interrupt the cycle. Thus,
you don't need to `try/catch` in your handler.

### Compile It

Once you have the code, compile it with GraalVM and Native image. The `Makefile`
of this repository has all the targets you need. You can borrow it with slight
changes. Here are the basic definitions:

```make
NI_TAG = ghcr.io/graalvm/native-image:22.2.0

JAR = target/uberjar/bootstrap.jar

PWD = $(shell pwd)

NI_ARGS = \
	--initialize-at-build-time \
	--report-unsupported-elements-at-runtime \
	--no-fallback \
	-jar ${JAR} \
	-J-Dfile.encoding=UTF-8 \
	--enable-http \
	--enable-https \
	-H:+PrintClassInitialization \
	-H:+ReportExceptionStackTraces \
	-H:Log=registerResource \
	-H:Name=bootstrap

uberjar:
	lein <...> uberjar

bootstrap-zip:
	zip -j bootstrap.zip bootstrap
```

Pay attention to the following:

- Ensure the jar name is set to `bootstrap.jar` in your project. This might be
  done by setting these in your `project.clj`:

```clojure
{:target-path "target/uberjar"
 :uberjar-name "bootstrap.jar"}
```

- The `NI_ARGS` might be extended with resources, e.g. if you want an EDN config
  file be baked into the binary file.

Then you compile the project either on Linux natively or with Docker.

#### Linux (Local Build)

On Linux, add the following Make targets:

```make
graal-build:
	native-image ${NI_ARGS}

build-binary-local: ${JAR} graal-build

bootstrap-local: uberjar build-binary-local bootstrap-zip
```

Then run `make bootstrap-local`. You'll get a file called `bootstrap.zip` with a single binary file `bootstrap` inside.

#### On MacOS (Docker)

On MacOS, add these targets:

```make
build-binary-docker: ${JAR}
	docker run -it --rm -v ${PWD}:/build -w /build ${NI_TAG} ${NI_ARGS}

bootstrap-docker: uberjar build-binary-docker bootstrap-zip
```

Then run `make bootstrap-docker` to get the same file but compiled in a Docker
image.

### Create a Lambda in AWS

Create a Lambda function in AWS. For the runtime, choose custom one called
`provided.al2` based on Amazon Linux 2. The architecture (x86_64/arm64) should
match the architecture of your machine. For example, as I build the project on
Mac M1, I choose arm64.

### Deploy and Test It

Upload the `bootstrap.zip` file from your machine. Being unzipped, the
`bootstrap` file is of a size of 25 megabytes. In zip, it's about 9 megabytes so
you can skip uploading it to S3 first.

Test you Lambda with the console to ensure it works.

## Ring Handler for HTTP Requests

The framework can turn HTTP events into Ring maps. There is a middleware that
transforms a your handler into a Ring handler. In the example below, pay
attention to the `ring/wrap-ring-event` middleware on the top of the stack. It
takes a JSON map that carries an HTTP event and transforms it into a Ring map,
then transforms a Ring response into AWS format.

Right after `ring/wrap-ring-event`, feel free to add any Ring middleware for
POST parameters, JSON, and so on.

```clojure
(ns demo.core
  (:require
   [lambda.ring :as ring]
   [lambda.main :as main]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]])
  (:gen-class))

(defn handler [request]
  (let [{:keys [request-method
                uri
                headers
                body]}
        request]

    {:status 200
     :body {:some {:data [1 2 3]}}}))

(def fn-event
  (-> handler
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (ring/wrap-ring-event)))

(defn -main [& _]
  (main/run fn-event))
```

## Sharing the State Between Events

In AWS, a Lambda can process several events if they happen at the same
time. Thus, it's useful to preserve the state between the handler calls. A state
can be a config map read from a resource or an open connection to some resource.

An easy way to keep the state is to close your handler function over some
variables. In this case, the handler is not a plain function but a function that
returns a function:

~~~clojure
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
~~~

The `make-handler` call builds a function closed over the `db` variable which
holds a persistent connection to a database. Under the hood, it calls the
`process-event` function which accepts the `db` as an argument. The connection
stays persistent and won't be created from scratch every time you process an
event. This, of course, applies only to a case when you have multiple events
that are served in series.
