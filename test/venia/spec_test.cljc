(ns venia.spec-test
  (:require [venia.spec :as vs]
    #?(:cljs [cljs.test :refer-macros [is are deftest testing]]
       :clj
            [clojure.test :refer :all])))

(deftest query->spec-simple-query
  (testing "Wrong data type - vector instead of map, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec []))))
  (testing "Correct data type, but map is empty, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {}))))
  (testing ":venia/queries is missing, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/operation {:operation/name "operation" :operation/type :query}}))))
  (testing ":venia/queries has wrong type - map instead of vector, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries {}}))))
  (testing ":venia/queries is empty, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries []}))))

  (testing "Query vector has only query name, should return conformed data."
    (is (= [:venia/query-def {:venia/queries [[:query/data {:query :queryName}]]}]
           (vs/query->spec {:venia/queries [[:queryName]]}))))

  (testing "Query vector has only fields, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec (vs/query->spec {:venia/queries [[[:x :y]]]})))))
  (testing "Query vector has non-keyword query name, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec (vs/query->spec {:venia/queries [["queryName" {:id 1} [:x :y]]]})))))
  (testing "Query vector has query name and args, but no fields. Should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec (vs/query->spec {:venia/queries [[:queryName {:id 1}]]})))))
  (testing "Query vector has args in wrong format, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries [[:queryName [:id 1] [:x :y]]]}))))
  (testing "Query vector has fields in wrong format, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries [[[:queryName {:id 1} {:x :y}]]]}))))
  (testing "Valid vector with single query, should return conformed data"
    (is (= [:venia/query-def {:venia/queries [[:query/data {:query  :employee
                                                            :args   {:id 1 :active true}
                                                            :fields [[:venia/field :name] [:venia/field :address]
                                                                     [:venia/nested-field {:venia/nested-field-root     :friends
                                                                                           :venia/nested-field-children [[:venia/field :name]
                                                                                                                         [:venia/field :email]]}]]}]]}]
           (vs/query->spec {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))
  (testing "Valid vector with single query and nested fragment, should return conformed data"
    (is (= [:venia/query-def {:venia/queries [[:query/data {:query  :employee
                                                            :args   {:id 1 :active true}
                                                            :fields [[:venia/field :name] [:venia/field :address]
                                                                     [:venia/nested-field {:venia/nested-field-root     :friends
                                                                                           :venia/nested-field-children [[:venia/field :name]
                                                                                                                         [:venia/field :email]]}]
                                                                     [:venia/nested-field-with-fragments {:venia/nested-field-root :pet
                                                                                                          :venia/fragments [:fragment/cat :fragment/dog]}]]}]]}]
           (vs/query->spec {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]] [:pet [:fragment/cat :fragment/dog]]]]]}))))
  (testing "Valid vector with single query and top level fragments, should return conformed data"
    (is (= [:venia/query-def {:venia/queries [[:query/data {:query  :employee
                                                            :args   {:id 1 :active true}
                                                            :fields [[:venia/fragments [:fragment/cat :fragment/dog]]]}]]}]
           (vs/query->spec {:venia/queries [[:employee {:id 1 :active true} [[:fragment/cat :fragment/dog]]]]}))))
  (testing ":venia/operation is missing name, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/operation {:operation/type :query}
                                                     :venia/queries   [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))
  (testing ":venia/operation is missing type, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/operation {:operation/name "name"}
                                                     :venia/queries   [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))
  (testing ":venia/operation has type, which is not supported, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/operation {:operation/type :mutation}
                                                     :venia/queries   [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))

  (testing ":venia/variables is empty, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/variables []
                                                     :venia/queries   [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))
  (testing ":venia/variables is missing name, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/variables [{:variable/type :query}]
                                                     :venia/queries   [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))
  (testing ":venia/variables is missing type, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/variables [{:variable/name "name"}]
                                                     :venia/queries   [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}))))
  (testing "Invalid fragment is used in query definition, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries   [[:employee {:id 1 :active true} :fragment/invalid]]
                                                     :venia/fragments [{:fragment/name   "comparisonFields"
                                                                        :fragment/type   :Worker
                                                                        :fragment/fields [[:venia/field :name] [:venia/field :address]
                                                                                          [:venia/nested-field {:venia/nested-field-root     :friends
                                                                                                                :venia/nested-field-children [[:venia/field :name]
                                                                                                                                              [:venia/field :email]]}]]}]}))))
  (testing "Undefined fragments are used in query definition, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries [[:employee {:id 1 :active true} :fragment/undefined]]}))))
  (testing "Invalid variable is used in query definition, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries   [[:employee {:id 1 :active :$invalid} [:name]]]
                                                     :venia/variables [{:variable/name "valid"
                                                                        :variable/type :Int}]}))))
  (testing "Undefined variables are used in query definition, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (vs/query->spec {:venia/queries [[:employee {:id 1 :active :$undefined} [:name]]]}))))
  (testing "Valid vector with all possible data, should return conformed data"
    (is (= [:venia/query-def {:venia/operation {:operation/type :query
                                                :operation/name "employeeQuery"}
                              :venia/variables [{:variable/name "id"
                                                 :variable/type [:type :Int]}
                                                {:variable/name "name"
                                                 :variable/type [:type :String]}]
                              :venia/fragments [{:fragment/name   "comparisonFields"
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
                            :venia/fragments [{:fragment/name   "comparisonFields"
                                               :fragment/type   :Worker
                                               :fragment/fields [:name :address [:friends [:name :email]]]}]})))))
