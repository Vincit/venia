(ns venia.core-test
  (:require [venia.core :as v]
    #?(:cljs [cljs.test :refer-macros [is are deftest testing]]
       :clj
            [clojure.test :refer :all])))

(deftest ArgumentFormatter-test
  (is (= "null" (v/arg->str nil)))
  (is (= "\"human\"" (v/arg->str "human")))
  (is (= "{id:1}" (v/arg->str {:id 1})))
  (is (= "{id:null}" (v/arg->str {:id nil})))
  (is (= "[1,2,3]" (v/arg->str [1 2 3])))
  (is (= "[1,{id:1},\"human\"]" (v/arg->str [1 {:id 1} "human"])))
  (is (= "human" (v/arg->str :human)))
  (is (= "1" (v/arg->str 1)))
  (is (= "{active:true}" (v/arg->str {:active true}))))

(deftest arguments->str-test
  (is (= "" (v/arguments->str {})))
  (is (= "id:1" (v/arguments->str {:id 1})))
  (is (= "id:null" (v/arguments->str {:id nil})))
  (is (= "id:1,type:\"human\"" (v/arguments->str {:id 1 :type "human"})))
  (is (= "id:1,vector:[1,2,3]" (v/arguments->str {:id 1 :vector [1 2 3]}))))

(deftest fields->str-test
  (is (= "name" (v/fields->str [[:venia/field :name]])))
  (is (= "name,address" (v/fields->str [[:venia/field :name] [:venia/field :address]])))
  (is (= "friends{name,email}" (v/fields->str [[:venia/nested-field {:venia/nested-field-root     :friends
                                                                     :venia/nested-field-children [[:venia/field :name]
                                                                                                   [:venia/field :email]]}]]))))

(deftest variables->str-test
  (is (= "$id:Int" (v/variables->str [{:variable/name "id"
                                       :variable/type :Int}])))
  (is (= "$id:Int,$name:String" (v/variables->str [{:variable/name "id"
                                                    :variable/type :Int}
                                                   {:variable/name "name"
                                                    :variable/type :String}])))
  (is (= "" (v/variables->str nil)))
  (is (= "" (v/variables->str []))))

(deftest fragment->str-test
  (is (= "fragment comparisonFields on Worker{name,address,friends{name,email}}"
         (v/fragment->str {:fragment/name   :comparisonFields
                           :fragment/type   :Worker
                           :fragment/fields [[:venia/field :name] [:venia/field :address]
                                             [:venia/nested-field {:venia/nested-field-root     :friends
                                                                   :venia/nested-field-children [[:venia/field :name]

                                                                                                 [:venia/field :email]]}]]}))))

(deftest graphql-query-test
  (testing "Should create a valid graphql string."
    (let [data {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]}
          query-str "{employee(id:1,active:true){name,address,friends{name,email}}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string using params on nested fields."
    (let [data {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends {:id 1} [:name :email]]]]]}
          query-str "{employee(id:1,active:true){name,address,friends(id:1){name,email}}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Invalid query, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (v/graphql-query []))))

  (testing "Should create a valid graphql string with alias"
    (let [data {:venia/queries [{:query/data  [:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]
                                 :query/alias :workhorse}
                                {:query/data  [:employee {:id 2 :active true} [:name :address [:friends [:name :email]]]]
                                 :query/alias :boss}]}
          query-str (str "{workhorse:employee(id:1,active:true){name,address,friends{name,email}},"
                         "boss:employee(id:2,active:true){name,address,friends{name,email}}}")]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql query with fragment"
    (let [data {:venia/queries   [{:query/data  [:employee {:id 1 :active true} :fragment/comparisonFields]
                                   :query/alias :workhorse}
                                  {:query/data  [:employee {:id 2 :active true} :fragment/comparisonFields]
                                   :query/alias :boss}]
                :venia/fragments [{:fragment/name   :comparisonFields
                                   :fragment/type   :Worker
                                   :fragment/fields [:name :address [:friends [:name :email]]]}]}
          query-str (str "{workhorse:employee(id:1,active:true){...comparisonFields},boss:employee(id:2,active:true){...comparisonFields}} "
                         "fragment comparisonFields on Worker{name,address,friends{name,email}}")
          result (v/graphql-query data)]
      (is (= query-str result))))

  (testing "Should create a valid graphql query with multiple fragments"
    (let [data {:venia/queries   [{:query/data  [:employee {:id 1 :active true} :fragment/comparisonFields]
                                   :query/alias :workhorse}
                                  {:query/data  [:employee {:id 2 :active true} :fragment/comparisonFields]
                                   :query/alias :boss}]
                :venia/fragments [{:fragment/name   :comparisonFields
                                   :fragment/type   :Worker
                                   :fragment/fields [:name :address [:friends [:name :email]]]}
                                  {:fragment/name   :secondFragment
                                   :fragment/type   :Worker
                                   :fragment/fields [:name]}]}
          query-str (str "{workhorse:employee(id:1,active:true){...comparisonFields},boss:employee(id:2,active:true){...comparisonFields}} "
                         "fragment comparisonFields on Worker{name,address,friends{name,email}},"
                         "fragment secondFragment on Worker{name}")
          result (v/graphql-query data)]
      (is (= query-str result))))

  (testing "Should create a valid graphql query with variables"
    (let [data {:venia/operation {:operation/type :query
                                  :operation/name "employeeQuery"}
                :venia/variables [{:variable/name "id"
                                   :variable/type :Int}
                                  {:variable/name "name"
                                   :variable/type :String}]
                :venia/queries   [[:employee {:id     :$id
                                              :active true
                                              :name   :$name}
                                   [:name :address [:friends [:name :email]]]]]}
          query-str (str "query employeeQuery($id:Int,$name:String){employee(id:$id,active:true,name:$name){name,address,friends{name,email}}}")
          result (v/graphql-query data)]
      (is (= query-str result))))

  (testing "Should create a valid graphql query with variables, aliases and fragments"
    (let [data {:venia/operation {:operation/type :query
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
                                   :fragment/fields [:name :address [:friends [:name :email]]]}]}
          query-str (str "query employeeQuery($id:Int,$name:String){workhorse:employee(id:$id,active:true,name:$name){...comparisonFields},"
                         "boss:employee(id:$id,active:false){...comparisonFields}} fragment comparisonFields on Worker{name,address,friends{name,email}}")
          result (v/graphql-query data)]
      (is (= query-str result)))))
