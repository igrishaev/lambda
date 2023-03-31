(defproject lambda-demo "0.1.0-SNAPSHOT"

  :description
  "FIXME: write description"

  :url
  "http://example.com/FIXME"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [http-kit "2.6.0"]
   [cheshire "5.10.0"]]

  :main ^:skip-aot lambda-demo.core

  :target-path "target/uberjar"

  :uberjar-name "lambda-demo.jar"

  :profiles
  {:uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
