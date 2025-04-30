(defproject com.github.igrishaev/lambda "0.1.3-SNAPSHOT"

  :description
  "AWS Lambda as single binary file"

  :url
  "https://github.com/igrishaev/lambda"

  :license
  {:name "The Unlicense"
   :url "https://unlicense.org/"}

  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :release-tasks
  [["vcs" "assert-committed"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :managed-dependencies
  [[org.clojure/clojure "1.11.1"]
   [http-kit "2.6.0"]
   [com.github.igrishaev/jsam "0.1.0"]
   [ring/ring-core "1.9.6"]
   [ring/ring-json "0.5.1"]]

  :dependencies
  [[org.clojure/clojure :scope "provided"]
   [http-kit]
   [com.github.igrishaev/jsam]]

  :target-path
  "target/uberjar"

  :uberjar-name
  "bootstrap.jar"

  :profiles
  {:demo1

   {:main demo1.main

    :dependencies
    [[ring/ring-core]
     [ring/ring-json]]

    :source-paths
    ["env/demo1/src"]}

   :dev
   {:dependencies
    [[ring/ring-core]
     [ring/ring-json]]
    :resource-paths ["env/dev/resources"]}

   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
