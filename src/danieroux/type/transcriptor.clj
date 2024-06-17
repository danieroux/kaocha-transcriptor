(ns danieroux.type.transcriptor
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]

   ;; https://github.com/cognitect-labs/transcriptor
            [cognitect.transcriptor :as xr]

            [kaocha.hierarchy]
            [kaocha.testable])
  (:import [java.io StringWriter]))

(def ^:dynamic *asserts*)

(defmacro check!
  "Delegate to `transcriptor/check!` and on success, mark it as a passing assert.

  This only exists for more granular reporting:

  You could also just use `transcriptor/check!` directly, in which case one .repl file will
  count as one test, with no assertions."
  {:arglists '([docstring? spec]
               [docstring? spec v])}
  ([& args]
   (let [has-doc? (string? (first args))
         args'    (if has-doc? (rest args) args)
         has-v?   (> (count args') 1)
         [spec v] args']
     (if-not has-v?
       `(check! ~spec *1)
       `(do
          (xr/check! ~spec ~v)
          (when (bound? #'*asserts*)
            (swap! *asserts* inc))
          (t/do-report {:type :pass}))))))

(defmethod kaocha.testable/-load :danieroux.type/transcriptor [testable]
  (let [test-paths   (:kaocha/test-paths testable)
        file-pattern (or
                       (re-pattern (:danieroux.type.transcriptor/file-pattern testable))
                       #".*")
        repl-files   (filter #(re-matches file-pattern %) (mapcat xr/repl-files test-paths))]
    (-> testable
      (assoc :kaocha.testable/desc "Transcriptor Tests")
      (assoc :kaocha.test-plan/tests
        (map
          (fn [repl-file]
            (let [the-repl-file-ns (second (read-string (slurp repl-file)))]
              {:kaocha.testable/type :danieroux.type/transcriptor-repl-file
               :kaocha.testable/id   (keyword the-repl-file-ns)
               :kaocha.testable/desc repl-file
               ::repl-file           repl-file}))
          repl-files))
      (dissoc :kaocha/tests))))

(defmethod kaocha.testable/-run :danieroux.type/transcriptor [testable test-plan]
  (t/do-report {:type ::begin-run})
  (let [results (kaocha.testable/run-testables (:kaocha.test-plan/tests testable) test-plan)]
    (t/do-report {:type ::end-run})
    (-> testable
      (assoc :kaocha.result/tests results)
      (dissoc :kaocha.test-plan/tests))))

(defmethod kaocha.testable/-run :danieroux.type/transcriptor-repl-file [testable _test-plan]
  (t/do-report {:type ::begin-repl-file})
  (let [repl-file                  (::repl-file testable)
        file-line-m                (fn file-line-m [ex]
                                     (zipmap [:file :line] (take-last 2 (:at (first (:via (Throwable->map ex)))))))
        the-out-captured-by-kaocha *out*
        sw                         (StringWriter.)]
    (binding [*out*     sw
              *asserts* (atom 0)]
      (try
        (xr/run repl-file)
        (t/do-report {:type ::end-repl-file})
        (assoc testable
          :kaocha.result/count 1
          :kaocha.result/pass @*asserts*
          :kaocha.result/fail 0)
        ; Because (assert) throws AssertionError, we have to catch this high
        ; Does not spark joy.
        (catch Throwable ex
          (binding [kaocha.testable/*test-location* (file-line-m ex)
                    *out*                           the-out-captured-by-kaocha]
            (let [message (ex-message ex)
                  actual  (or (ex-data ex) ex)]
              (t/do-report {:type    ::fail-repl-file-test
                            :message message
                            :actual  actual})
              (print (str sw))))
          (assoc testable
            :kaocha.result/count 1
            :kaocha.result/pass @*asserts*
            :kaocha.result/fail 1
            :koacha.result/err (ex-message ex)))))))

(s/def :danieroux.type/transcriptor (s/keys :req [:kaocha/test-paths]))
(s/def :danieroux.type/transcriptor-repl-file (s/keys :req [::repl-file]))

(kaocha.hierarchy/derive! :danieroux.type/transcriptor :kaocha.testable.type/suite)
(kaocha.hierarchy/derive! :danieroux.type/transcriptor-repl-file :kaocha.testable.type/leaf)

(kaocha.hierarchy/derive! ::begin-run :kaocha/begin-suite)
(kaocha.hierarchy/derive! ::end-run :kaocha/end-suite)

(kaocha.hierarchy/derive! ::begin-repl-file :kaocha/begin-test)
(kaocha.hierarchy/derive! ::fail-repl-file-test :kaocha/fail-type)
(kaocha.hierarchy/derive! ::end-repl-file :kaocha/end-test)

(comment
  (require 'kaocha.specs)

  (def a-testable {:kaocha.testable/type :danieroux.type/transcriptor
                   :kaocha.testable/id   :transcriptor
                   :kaocha/source-paths  ["src"]
                   :kaocha/test-paths    ["src"]})

  (s/valid? :kaocha/testable a-testable)

  (use 'kaocha.repl)

  (run)

  (config)
  (test-plan)

  ())

