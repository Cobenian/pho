(ns pho.core
  (:require [pantomime.mime :refer [mime-type-of]])
  (:require [aws.sdk.s3 :as s3])
  (:require [clojure.tools.cli :refer [cli]])
  (:gen-class))


;;(def cred {:access-key "foof", :secret-key "kv7tfoofkey+w1WX"})

;; Mime type detection by filename
;;(mime-type-of "/Users/adam/Projects/demoit/emberit/dist/index.html")

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
;;;;
;; This is really bad, I hardcoded stuff...
;; transform into a transducer

;;(defn get-logs []
 ;; (filter (fn [i] (.contains i "logs")) (pull-fnames-by-bucket cred "www.demoit.io")))

;;get the log files
;;(dump-fnames-to-file "/tmp/logs4.txt" (get-logs))

;; delete logs
;;(defn delete-logs [creds bucket-name]
;;  (map (fn [key] (s3/delete-object creds bucket-name key)) (get-logs)))

;; Didn't delete them all for some reason
;;(delete-logs cred "www.foofer.io")

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
            ["-h" "--help" "specify --creds <file-with-creds> --bucket <bucketname> --sync|--delete"  :default false :flag true]
            ["-b" "--bucket" "Bucket Name"]
            ["-c" "--creds" "Credential File"]
            ["-s" "--sync" "Sync cwd and below to S3" :flag true]
            ["-d" "--delete" "Delete the files on S3" :flag true])]
    (when (:help opts)
      (println banner))
    (when (:sync opts)
      (doall (push-sync (grab-creds (:creds opts)) (:bucket opts) ".")))
    (when (:delete opts)
      (doall (delete-all! (grab-creds (:creds opts)) (:bucket opts))))
    (println "Exiting all done now"))
    (catch Exception e
      (println "Exception: " e)
      ((System/exit 0)))))


