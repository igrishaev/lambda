
all: lint test

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

.PHONY: clear
clear:
	rm -rf target
	rm -rf reports

repl:
	lein repl

graal-build:
	native-image ${NI_ARGS}

build-binary-docker: ${JAR}
	docker run -it --rm -v ${PWD}:/build -w /build ${NI_TAG} ${NI_ARGS}

build-binary-local: ${JAR} graal-build

uberjar:
	lein with-profile +demo1 uberjar

bootstrap-zip:
	zip -j bootstrap.zip bootstrap

bootstrap-docker: clear uberjar build-binary-docker bootstrap-zip

bootstrap-local: uberjar build-binary-local bootstrap-zip

lint:
	clj-kondo --lint src test
	cljfmt check

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

.PHONY: test
test:
	lein test

release: lint test
	lein release
