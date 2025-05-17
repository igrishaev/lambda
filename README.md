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
  * [Configuration](#configuration)
  * [Deploy and Test It](#deploy-and-test-it)
- [Ring Support (Serving HTTP events)](#ring-support-serving-http-events)
- [Gzip Support](#gzip-support)
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
[com.github.igrishaev/lambda "0.1.4"]
```

Clojure CLI/deps.edn

```
com.github.igrishaev/lambda {:mvn/version "0.1.4"}
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
catches an exception and reports it as well without interrupt the cycle. Thus,
you don't need to `try/catch` in your handler.

### Compile It

Once you have the code, compile it with GraalVM and Native image. The `Makefile`
of this repository has all the targets you need. You can borrow them with slight
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
  file baked into the binary file.

Then compile the project either on Linux natively or with Docker.

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

Run `make bootstrap-docker` to get the same file but compiled in a Docker
image.

### Create a Lambda in AWS

Create a Lambda function in AWS. For the runtime, choose a custom one called
`provided.al2` which is based on Amazon Linux 2. The architecture (x86_64/arm64)
should match the architecture of your machine. For example, as I build the
project on Mac M1, I choose arm64.

### Configuration

There are some options you can override with environment variables, namely:

| Var                      | Default          | Comment                                         |
|--------------------------|------------------|-------------------------------------------------|
| `LAMBDA_RUNTIME_TIMEOUT` | 900000 (15 mins) | How long to wait when polling for a new event   |
| `LAMBDA_RUNTIME_VERSION` | 2018-06-01       | Which Runtime API version to use                |
| `AWS_LAMBDA_USE_GZIP`    | nil              | Forcibly gzip-encode Ring responses (see below) |

### Deploy and Test It

Upload the `bootstrap.zip` file from your machine to the lambda. With no
compression, the `bootstrap` file takes 25 megabytes. In zip, it's about 9
megabytes so you can skip uploading it to S3 first.

Test you Lambda in the console to ensure it works.

## Ring Support (Serving HTTP events)

AWS Lambda can serve HTTP requests as events. Each HTTP request gets transformed
into a special message which your lambda processes. It must return another
message that forms an HTTP response.

[ring]: https://github.com/ring-clojure/ring

This library brings a number of middleware that turn a lambda into
[Ring-compatible][ring] HTTP server.

There are the following middleware wrappers in the `lambda.ring` namespace:

- `wrap-ring-event`: turns an incoming HTTP event into a Ring request map,
  processes it and turns a Ring response map into an Lambda-compatible HTTP
  message.

- `wrap-ring-exception`: captures any uncaught exception happened while handling
  an HTTP request. Log it and return an error response (500 Internal server
  error).

[ring-json]: https://github.com/ring-clojure/ring-json

To not depend on [ring-json][ring-json] (which in turn depends on Cheshire), we
provide our own tree middlware for incoming and outcoming JSON:

- `wrap-json-body`: if the request was JSON, replace the `:body` field with
  a parsed payload.

- `wrap-json-params`: the same but puts the data into the `:json-params`
  field. In addition, if the data was a map, merge it into the `:params` map.

- `wrap-json-response`: if the body of the response was a collection, encode it
  into a JSON string and add the Content-Type: application/json header.

[jsam]: https://github.com/igrishaev/jsam

These three middleware mimic their counterparts from Ring-json but rely on the
JSam library to keep dependencies as narrow as possible. Each middleware, in
addition to a ring handler, accepts an optional map of JSON settings.

The following example shows how to build a stack of middleware properly:

~~~clojure
(ns some.demo
  (:gen-class)
  (:require
   [lambda.main :as main]
   [lambda.ring :as ring]))

(defn handler [request]
  (let [{:keys [request-method
                uri
                headers
                body]}
        request]
    ;; you can branch depending on method and uri,
    ;; or use compojure/reitit
    {:status 200
     :headers {"foo" "bar"}
     :body {:some "JSON date"}}))

(def fn-event
  (-> handler
      (ring/wrap-json-body)
      (ring/wrap-json-response)
      (ring/wrap-ring-exception)
      (ring/wrap-ring-event)))

(defn -main [& _]
  (main/run fn-event))
~~~

For query- or form parameters, you can use classic `wrap-params`,
`wrap-keyword-params`, and similar utilities from `ring.middleware.*`
namespaces. For this, introduce the `ring-core` library into your project.

## Gzip Support for Ring

The library provides a special Ring middlware to handle gzip logic. Apply it as
follows:

~~~clojure
(def fn-event
  (-> handler
      (ring/wrap-json-body)
      (ring/wrap-json-response)
      (ring/wrap-gzip) ;; -- this
      (ring/wrap-ring-exception)
      (ring/wrap-ring-event)))
~~~

This is what the middleware does under the hood:

- if a client sends a gzipped payload and the `Content-Encoding` header is
  `gzip`, the incoming `:body` field gets wrapped with the `GzipInputStream`
  class. By reading from it, you'll get the origin payload. Useful when sending
  vast JSON objects to Lambda via HTTP.

- If a client sends a header `Accept-Encoding` with `gzip` inside, the body of a
  response gets gzipped, and the `Content-Encoding: gzip` header is set. It
  greatly saves traffic. In addition, remember about a limitation in AWS: a
  response cannot exceed 6Mbs. Gzipping helps bypass this limit.

- If there is a non-empty env var `AWS_LAMBDA_USE_GZIP` set for this Lambda, the
  response is always gzipped no matter what client specifies in the
  `Accept-Encoding` header.

Although enabling gzip looks trivial, missing it might lead to very strange
things. Personally I spent several a couple of days investigating an issue when
AWS says "the content was too large". Turned out, the culprit was **double JSON
encoding**. When you return JSON from Ring, you encode it once. But when Lambda
runtime sends this message to AWS, it gets JSON-encoded again. This adds extra
slashes and blows up payload by 15-20%. For details, see these pages:

- [A StackOverflow question with my answer](https://stackoverflow.com/questions/66971400/aws-lambda-body-size-is-too-large-error-but-body-size-is-under-limit)
- [A question on AWS:repost with no answer](https://repost.aws/questions/QU57r4NMQIQROXqW4Vl6YDBQ)
- [My blog post (in Russian, use Google Translate)](https://grishaev.me/aws-1/)

## Sharing the State Between Events

In AWS, a Lambda can process several events if they happen in series. Thus, it's
useful to preserve the state between the handler calls. A state can be a config
map read from a resource or an open connection to some resource.

An easy way to share the state is to close your handler function over some
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
served in series.

~~~
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
Ivan Grishaev, 2025. © UNLICENSE ©
©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©©
~~~
