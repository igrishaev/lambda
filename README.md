# Lambda

A small framework to run AWS Lambdas compiled with Native Image.

## Table of Contents

<!-- toc -->

- [Motivation & Benefits](#motivation--benefits)
- [Installation](#installation)
- [Making Your First Lambda](#making-your-first-lambda)
  * [Prepare The Code](#prepare-the-code)
  * [Compile It](#compile-it)
  * [Create a Lambda in AWS](#create-a-lambda-in-aws)
  * [Deploy and Test It](#deploy-and-test-it)
- [Ring Handler for HTTP Requests](#ring-handler-for-http-requests)
- [Sharing the State](#sharing-the-state)

<!-- tocstop -->

## Motivation & Benefits

[search]: https://clojars.org/search?q=lambda

There is a lot of Lambda Clojure libraries so far: quick search on Clojars gives
several screens of them. What is the point to make a new one? Well, because none
of the existing libraries covers my requirements, namely:

- I want a framework be free from any Java SDK, but pure Clojure only.
- I want it to compile into a single binary file so no environment of any kind
  is needed.
- The deployment process must be extremely simple.

As the result, this framework:

- Depends only on Http Kit and Cheshire for interaction with AWS;
- Provides an endless loop that consumes events from AWS and handles them. You
  only submit a function that processes an event.
- Provides a Ring middleware that turns HTTP events into a Ring handler. Thus,
  you can easily serve HTTP requests with the good old Ring stack.
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

## Making Your First Lambda

### Prepare The Code

### Compile It

### Create a Lambda in AWS

### Deploy and Test It

## Ring Handler for HTTP Requests

## Sharing the State
