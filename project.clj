(defproject com.github.igrishaev/lambda "0.1.0-SNAPSHOT"

  :description
  "An AWS Lambda in a single binary file"

  :url
  "https://github.com/igrishaev/lambda"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [http-kit "2.6.0"]
   [cheshire "5.10.0"]]

  :target-path
  "target/uberjar"

  :uberjar-name
  "bootstrap.jar"

  :profiles
  {:demo1

   {:main demo1.main

    :dependencies
    [[ring/ring-core "1.9.6"]
     [ring/ring-json "0.5.1"]]

    :source-paths
    ["env/demo1/src"]}

   :dev
   {:dependencies
    [[ring/ring-core "1.9.6"]
     [ring/ring-json "0.5.1"]]
    :resource-paths ["env/dev/resources"]}

   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
