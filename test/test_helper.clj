(ns test-helper
  (:require [clojure.test :refer [is testing]]
            [clojure.test.check :as tc]
            [clojure.string :as s]
            [clojure.test.check.properties :as prop]))

(def difficulties #{:elementary :easy :medium :hard})

(defn- check-output-type [data]
  (if (or (= (type data) java.lang.Integer) (= (type data) java.lang.Long)) ; for backward compatibility 
    java.lang.Long
    (type data)))

(defn prepare-signature [signature]
  (map #(dissoc % :argument-name) (signature :input)))

(defn- nested? [element]
  (contains?
   #{clojure.lang.PersistentList
     clojure.lang.PersistentArrayMap
     clojure.lang.PersistentVector}
   (type element)))

(def type-map
  {java.lang.String "string"
   java.lang.Long "integer"
   java.lang.Integer "integer"
   java.lang.Double "float"
   java.lang.Boolean "boolean"
   clojure.lang.Ratio "integer"
   clojure.lang.PersistentList "array"
   clojure.lang.PersistentVector "array"
   clojure.lang.PersistentArrayMap "hash"
   clojure.lang.Keyword "string"})

(defn- type-nested [elem]
  (let [elem-type (type-map (type elem))
        head (if (= elem-type "hash")
               (first (vals elem))
               (first elem))]
    (if (nested? head)
      {:name elem-type :nested (type-nested head)}
      {:name elem-type :nested {:name (type-map (type head))}})))

(defn- type-element [elem]
  {:type (if (nested? elem)
           (type-nested elem)
           {:name (type-map (type elem))})})

(defn prepare-expected-results [expected]
  (list (type-element expected)))

(defn prepare-arguments [arguments]
  (map type-element arguments))

(defn generate-data-tests [data signature]
  (let [input-signature (prepare-signature signature)
        output-signature (list (signature :output))]
    (doseq [{expected :expected arguments :arguments} data]
      (let [prepared-expected (prepare-expected-results expected)
            prepared-args (prepare-arguments arguments)]
        (is (= prepared-args input-signature))
        (is (= prepared-expected output-signature))))))

(defn run-difficulty-test
  "Tests that checking is task difficulty correct."
  [difficulty issue-name]
  (testing (str "Difficulty for " issue-name " task.")
    (is (true? (contains? difficulties difficulty)))))

(defn run-solution-test
  "Test that checking is test data solves correctly."
  [data solution issue-name]
  (testing (str "Solution for " issue-name " task.")
    (doseq [{expected :expected arguments :arguments} data]
      (is (= expected (apply solution arguments))))))

(defn run-test-data-spec-test
  "Test that checking is test data corresponds to signature."
  [test-data signature issue-name]
  (testing (str "Test-data and signature for " issue-name " task.")
    (generate-data-tests test-data signature)))

(defn run-generator-spec-test
  "Test that checks if generated data matches issue signature."
  [arguments-generator signature issue-name]
  (testing (str "Generated spec and described spec for " issue-name " task.")
    (tc/quick-check 20 (prop/for-all [v (arguments-generator)]
                                     (is (= (prepare-signature signature) (prepare-arguments v)))))))

(def type-check-map
  {"string" String
   "integer" java.lang.Long
   "float" java.lang.Double
   "boolean" Boolean
   "array" clojure.lang.PersistentVector
   "hash" clojure.lang.PersistentArrayMap})

(defn run-solution-spec-test
  "Tests that checking is result type of solving issue is correct."
  [arguments-generator signature solution issue-name]
  (testing (str "Solution spec for " issue-name " task.")
    (let [output-type (type-check-map (((signature :output) :type) :name))]
      (tc/quick-check 20 (prop/for-all [v (arguments-generator)]
                                       (let [actual-output-type
                                             (check-output-type (apply solution v))]
                                         (is (= output-type actual-output-type))))))))

(defn run-description-test
  "Test that checking is issue has minimal description."
  [description issue-name]
  (testing (str "Test minimal description lang for " issue-name " task.")
    (is (true? (and
                (contains? description :en)
                (not (s/blank? (description :en))))))))
