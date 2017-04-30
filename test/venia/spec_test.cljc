(ns venia.spec-test
  (:require [venia.spec :as vs]
    #?(:cljs [cljs.test :refer-macros [is are deftest testing]]
       :clj
            [clojure.test :refer :all])))

(deftest query->spec-simple-query
  "Simple vector queries"
  (testing "Empty vector, should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec []))))
  (testing "Vector has only query name, should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec [[:queryName]]))))
  (testing "Vector has only fields, should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec [[[:x :y]]]))))
  (testing "Vector has non-keyword query name, should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec [["queryName" {:id 1} [:x :y]]]))))
  (testing "Vector has query name and args, but no fields. Should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec [[:queryName {:id 1}]]))))
  (testing "Vector has args in wrong format, should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec [[:queryName [:id 1] [:x :y]]]))))
  (testing "Vector has fields in wrong format, should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec [[:queryName {:id 1} {:x :y}]]))))
  (testing "Inner vector missing, should not be valid"
    (is (= #?(:clj :clojure.spec.alpha/invalid
              :cljs :cljs.spec/invalid) (vs/query->spec [[:queryName {:id 1} [:x :y]]]))))
  (testing "Valid vector with single query, should return conformed data"
    (is (= [[:venia/query-vector
             [{:args   {:id 1}
               :fields [[:venia/field
                         :x]
                        [:venia/field
                         :y]]
               :query  :queryName}]]] (vs/query->spec [[[:queryName {:id 1} [:x :y]]]]))))
  (testing "Valid vector with two queries, should return conformed data"
    (is (= [[:venia/query-vector
             [{:args   {:id 1}
               :fields [[:venia/field
                         :x]
                        [:venia/field
                         :y]]
               :query  :queryName}
              {:args   {:name "abc"}
               :fields [[:venia/field
                         :i]
                        [:venia/field
                         :k]]
               :query  :otherQuery}]]] (vs/query->spec [[[:queryName {:id 1} [:x :y]]
                                                         [:otherQuery {:name "abc"} [:i :k]]]])))))