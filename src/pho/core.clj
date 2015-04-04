(ns pho.core
  (:require [pantomime.mime :refer [mime-type-of]])
  (:require [aws.sdk.s3 :as s3])
  (:require [clojure.tools.cli :refer [cli]])
  (:import com.amazonaws.services.cloudfront.AmazonCloudFrontClient
           com.amazonaws.auth.BasicAWSCredentials
           com.amazonaws.ClientConfiguration
           com.amazonaws.services.cloudfront.model.ListDistributionsRequest
           com.amazonaws.services.cloudfront.model.ListDistributionsResult
           com.amazonaws.services.cloudfront.model.DistributionList
           com.amazonaws.services.cloudfront.model.DistributionSummary
           com.amazonaws.services.cloudfront.model.CreateInvalidationRequest
           com.amazonaws.services.cloudfront.model.CreateInvalidationResult
           com.amazonaws.services.cloudfront.model.Invalidation
           com.amazonaws.services.cloudfront.model.InvalidationBatch
           com.amazonaws.services.cloudfront.model.Paths)
  (:gen-class))

;;from: http://stackoverflow.com/q/16748447
;; we don't need a uuid for this
(defn gen-string[chunk-size, key-length]
 (apply str
  (flatten
   (interpose "-"
    (partition chunk-size
     (take key-length
      (repeatedly #(rand-nth "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"))))))))

(defn build-cf-client [creds]
  (AmazonCloudFrontClient. (BasicAWSCredentials. (:access-key creds) (:secret-key creds))))

(defn list-distros [creds]
  (println "Listing distros...")
  (for [distro (.getItems (.getDistributionList
                  (.listDistributions (build-cf-client creds) (ListDistributionsRequest.))))]
        (println "Distro: " (.getId distro))))
;;
   ;;(list-distros (grab-creds "/Users/adam/.s3.cobenian"))


(defn build-index-path []
  (let [index-path (Paths.)]
    (.setItems index-path (java.util.ArrayList. ["/index.html"]))
    (.setQuantity index-path (Integer. 1))
    index-path))

(defn invalidate-index [creds which-distro]
  (let [cf-client (build-cf-client creds)
        index-path (build-index-path)]
   (print "Invalidating index.html with ID: "
     (.getId
       (.getInvalidation
         (.createInvalidation cf-client
           (CreateInvalidationRequest. which-distro
             (InvalidationBatch. index-path (gen-string 12 13)))))))))

;; Returns a list of the filenames in the bucket
(defn pull-fnames-by-bucket [creds bucket-name]
   (map (fn [i] (:key i))
        (:objects (s3/list-objects creds bucket-name))))

;; dumps filenames to a specified file
;;(dump-fnames-to-file "/tmp/foo.txt3" (pull-fnames-by-bucket cred "www.demoit.io"))
(defn dump-fnames-to-file [some-filename some-seq]
  (with-open [w (clojure.java.io/writer some-filename)]
    (doseq [line some-seq]
      (.write w line)
        (.newLine w))))

;;;;;;;; Util Logs stuff -- very Cobenian specific
;; This is really bad, I hardcoded stuff...
;; transform into a transducer
;;(defn get-logs []
 ;; (filter (fn [i] (.contains i "logs")) (pull-fnames-by-bucket cred "www.demoit.io")))
;;get the log files
;;(dump-fnames-to-file "/tmp/logs4.txt" (get-logs))
;; delete logs
;;(defn delete-logs [creds bucket-name]
;;  (map (fn [key] (s3/delete-object creds bucket-name key)) (get-logs)))

;; simply note it
(defn note [file-name]
  (println "Deleteing " file-name)
    file-name)

;; Now we need a delete all
(defn delete-all! [creds bucket-name]
  (println "Deleting all files on: " bucket-name ", please be patient...")
  (map #(s3/delete-object creds bucket-name (note %)) (pull-fnames-by-bucket creds bucket-name)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Local file sync
(defn get-names [file-s]
  (map #(.getPath %) file-s))

(defn just-files [file-s]
  (filter #(.isFile %) file-s))

(defn local-list [some-dir]
  (get-names (just-files (file-seq (clojure.java.io/file some-dir)))))

;; Removes the ./ if it appears in the key name
(defn clean-if-dot [some-file-name]
  (if (.startsWith some-file-name "./")
    (subs some-file-name 2)
    some-file-name))

;; Filter out the dot (./) from the some-file
(defn push-file [creds bucket-name some-file]
  (println "pushing file: " some-file)
  (let [file-content (slurp some-file)]
    (s3/put-object creds bucket-name (clean-if-dot some-file) file-content {:content-type (mime-type-of some-file)})))

;; example of push
(defn push-sync [creds bucket-name some-dir]
  (println "Pushing to bucket: " bucket-name)
  (map #(push-file creds bucket-name %) (local-list some-dir)))

;; Grab the users credntials
(defn grab-creds [file-name]
      (zipmap [:access-key :secret-key](clojure.string/split (slurp file-name) #"\s+")))

;; Test Push
;;(push-sync (grab-creds "/Users/adam/.s3.cobenian") "test.push" ".")
;; test delete .. works!
;;(delete-all! (grab-creds "/Users/adam/.s3.cobenian") "test.push")

(defn -main [& args]
  (try
  (let [[opts args banner]
          (cli args
            ["-h" "--help" "specify --creds=<file-with-creds> --bucket=<bucketname> [--invalidate-index=<CloudFrontDistroId> --list-distros] --sync|--delete"  :default false :flag true]
            ["-b" "--bucket" "Bucket Name"]
            ["-c" "--creds" "Credential File"]
            ["-i" "--invalidate-index" "Invalidation Request"]
            ["-l" "--list-distros" "List Distros" :flag true]
            ["-s" "--sync" "Sync cwd and below to S3" :flag true]
            ["-d" "--delete" "Delete the files on S3" :flag true])]
    (when (:help opts)
      (println banner))
    (when (:sync opts)
      (doall (push-sync (grab-creds (:creds opts)) (:bucket opts) ".")))
    (when (:invalidate-index opts)
      (doall (invalidate-index (grab-creds (:creds opts)) (:invalidate-index opts))))
    (when (:delete opts)
      (doall (delete-all! (grab-creds (:creds opts)) (:bucket opts))))
    (when (:list-distros opts)
      (doall (list-distros (grab-creds (:creds opts)))))
    (println "Exiting all done now"))
    (catch Exception e
      (println "Exception: " e)
      ((System/exit 0)))))


