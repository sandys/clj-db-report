(defproject vertica-report "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :disable-implicit-clean true
  ;:jvm-opts "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8030"
  ;:repl-retry-limit 10000
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring "0.3.10"]
                 [enlive "1.0.0"]
                 [net.cgrand/moustache "1.0.0"]])
