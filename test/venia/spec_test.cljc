(ns venia.spec-test
  (:require [venia.spec :as vs]
    #?(:cljs [cljs.test :refer-macros [is are deftest testing]]
       :clj
            [clojure.test :refer :all])))

(deftest query->spec-simple-query
  "Simple vector queries"
  (testing "Empty vector, should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec []))))
  (testing "Vector has only query name, should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec [[:queryName]]))))
  (testing "Vector has only fields, should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec [[[:x :y]]]))))
  (testing "Vector has non-keyword query name, should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec [["queryName" {:id 1} [:x :y]]]))))
  (testing "Vector has query name and args, but no fields. Should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec [[:queryName {:id 1}]]))))
  (testing "Vector has args in wrong format, should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec [[:queryName [:id 1] [:x :y]]]))))
  (testing "Vector has fields in wrong format, should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec [[:queryName {:id 1} {:x :y}]]))))
  (testing "Inner vector missing, should not be valid"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec [[:queryName {:id 1} [:x :y]]]))))
  (testing "Valid vector with single query, should return conformed data"
    (is (= [:venia/query-def {:venia/queries [[:query/data {:query  :employee
                                                            :args   {:id 1 :active true}
                                                            :fields [[:venia/field :name] [:venia/field :address]
                                                                     [:venia/nested-field {:venia/nested-field-root     :friends
                                                                                           :venia/nested-field-children [[:venia/field :name]
                                                                                                                         [:venia/field :email]]}]]}]]}]
           (vs/query->spec {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))
  (testing "Valid vector with all possible data, should return conformed data"
    (is (= [:venia/query-def {:venia/operation {:operation/type :query
                                                :operation/name "employeeQuery"}
                              :venia/variables [{:variable/name "id"
                                                 :variable/type :Int}
                                                {:variable/name "name"
                                                 :variable/type :String}]
                              :venia/fragments [{:fragment/name   :comparisonFields
                                                 :fragment/type   :Worker
                                                 :fragment/fields [[:venia/field :name] [:venia/field :address]
                                                                   [:venia/nested-field {:venia/nested-field-root     :friends
                                                                                         :venia/nested-field-children [[:venia/field :name]
                                                                                                                       [:venia/field :email]]}]]}]
                              :venia/queries   [[:venia/query-with-data {:query/data  {:query  :employee
                                                                                       :args   {:id     :$id
                                                                                                :active true
                                                                                                :name   :$name}
                                                                                       :fields :fragment/comparisonFields}
                                                                         :query/alias :workhorse}]
                                                [:venia/query-with-data {:query/data  {:query  :employee
                                                                                       :args   {:id     :$id
                                                                                                :active false}
                                                                                       :fields :fragment/comparisonFields}
                                                                         :query/alias :boss}]]}]
           (vs/query->spec {:venia/operation {:operation/type :query
                                              :operation/name "employeeQuery"}
                            :venia/variables [{:variable/name "id"
                                               :variable/type :Int}
                                              {:variable/name "name"
                                               :variable/type :String}]
                            :venia/queries   [{:query/data  [:employee {:id     :$id
                                                                        :active true
                                                                        :name   :$name}
                                                             :fragment/comparisonFields]
                                               :query/alias :workhorse}
                                              {:query/data  [:employee {:id     :$id
                                                                        :active false}
                                                             :fragment/comparisonFields]
                                               :query/alias :boss}]
                            :venia/fragments [{:fragment/name   :comparisonFields
                                               :fragment/type   :Worker
                                               :fragment/fields [:name :address [:friends [:name :email]]]}]})))))