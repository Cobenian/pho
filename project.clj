(defproject pho "0.2.0-SNAPSHOT"
  :description "An Amazon S3 File Syncer"
  :url "http://cobenian.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/tools.cli "0.3.1"]
                 [com.amazonaws/aws-java-sdk "1.7.5" :exclusions [joda-time]]
                 [clj-aws-s3 "0.3.10"]
                 [com.novemberain/pantomime "2.4.0"]
                 [org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot pho.core
  :target-path "target/%s"
  :bin { :name "pho" }
  :profiles {:uberjar {:aot :all}})
