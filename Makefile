
NI_TAG = ghcr.io/graalvm/native-image:22.2.0

JAR = target/uberjar/lambda-demo.jar

PWD = $(shell pwd)

PLATFORM = PLATFORM

NI_ARGS = \
	--initialize-at-build-time \
	--report-unsupported-elements-at-runtime \
	--no-fallback \
	-jar ${JAR} \
	-J-Dfile.encoding=UTF-8 \
	--enable-http \
	--enable-https \
	-H:+PrintClassInitialization \
	# -H:ReflectionConfigurationFiles=reflection-config.json \
	-H:+ReportExceptionStackTraces \
	-H:Log=registerResource \
	-H:Name=./builds/lambda-demo-


platform-docker:
	docker run -it --rm --entrypoint /bin/sh ${NI_TAG} -c 'echo `uname -s`-`uname -m`' > ${PLATFORM}


build-binary-docker: ${JAR} platform-docker
	docker run -it --rm -v ${PWD}:/build -w /build ${NI_TAG} ${NI_ARGS}$(shell cat ${PLATFORM})

uberjar:
	lein uberjar
