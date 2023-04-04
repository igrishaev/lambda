# lambda-demo

## Installation

Leiningen/Boot

```
[com.github.igrishaev/lambda "0.1.0"]
```

Clojure CLI/deps.edn

```
com.github.igrishaev/lambda {:mvn/version "0.1.0"}
```


```
make uberjar
make build-binary-docker

mv Linux-aarch64 lambda-demo
chmod +x lambda-demo
```



```
START RequestId: efbaf029-60e3-49e5-b36d-708f7b4157ba Version: $LATEST
headers: {:content-length "49", :content-type "application/json", :date "Fri, 31 Mar 2023 15:53:51 GMT", :lambda-runtime-aws-request-id "efbaf029-60e3-49e5-b36d-708f7b4157ba", :lambda-runtime-deadline-ms "1680278034470", :lambda-runtime-invoked-function-arn "arn:aws:lambda:us-east-1:474692903177:function:asdf", :lambda-runtime-trace-id "Root=1-6427020f-759ca95f467960bb063adbeb;Parent=42ae836531ace214;Sampled=0"}
event-data: {"key1" "value1", "key2" "value2", "key3" "value3"}
status: 202
body: {"status":"OK"}
END RequestId: efbaf029-60e3-49e5-b36d-708f7b4157ba
REPORT RequestId: efbaf029-60e3-49e5-b36d-708f7b4157ba	Duration: 1.30 ms	Billed Duration: 2 ms	Memory Size: 128 MB	Max Memory Used: 57 MB
```
