# Lambda

A small framework to run AWS Lambdas compiled with Native Image.

## Table of Contents

<!-- toc -->

- [Motivation & Benefits](#motivation--benefits)
- [Installation](#installation)
- [Writing Your First Lambda](#writing-your-first-lambda)
  * [Prepare The Code](#prepare-the-code)
  * [Error Handling](#error-handling)
  * [Compile It](#compile-it)
    + [On Linux (Locally)](#on-linux-locally)
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

As the result, this framework:

- Depends only on Http Kit and Cheshire for interaction with AWS;
- Provides an endless loop that consumes events from AWS and handles them. You
  only submit a function that processes an event.
- Provides a Ring middleware that turns HTTP events into a Ring handler. Thus,
  you can easily serve HTTP requests with Ring stack.
- Has a built-in logging facility.
- Provides a bunch of Make commands to build a zipped bootstrap file.

## Installation

Leiningen/Boot

```
[com.github.igrishaev/lambda "0.1.0"]
```

Clojure CLI/deps.edn

```
com.github.igrishaev/lambda {:mvn/version "0.1.0"}
```

## Writing Your First Lambda

### Prepare The Code

```clojure
(ns demo.main
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

### Error Handling

### Compile It

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
	lein with-profile +demo1 uberjar

bootstrap-zip:
	zip -j bootstrap.zip bootstrap
```

#### On Linux (Locally)

```make
graal-build:
	native-image ${NI_ARGS}

build-binary-local: ${JAR} graal-build

bootstrap-local: uberjar build-binary-local bootstrap-zip
```

#### On MacOS (Docker)

```make

build-binary-docker: ${JAR}
	docker run -it --rm -v ${PWD}:/build -w /build ${NI_TAG} ${NI_ARGS}

bootstrap-docker: uberjar build-binary-docker bootstrap-zip
```

### Create a Lambda in AWS

### Deploy and Test It

## Ring Handler for HTTP Requests

```clojure
(ns demo1.main
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
