(ns venia.core-test
  (:require [clojure.string :as string]
            [venia.core :as v]
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
  (is (= "{active:true}" (v/arg->str {:active true})))
  (let [value (hash-map :a 0 :b 1 :c 2)
        output (v/arg->str value)]
    (is (and (string/starts-with? output "{")
             (string/ends-with? output "}")))
    (is (= (set ["a:0" "b:1" "c:2"])
           (-> output
               (string/replace #"^.|.$" "")
               (string/split #",")
               (set)))))
  ;; List in cljs
  (is (= "[1,2,3]" (v/arg->str '(1 2 3))))
  ;; IndexedSeq in cljs
  (is (= "[1,2,3]" (v/arg->str (seq [1 2 3]))))
  ;; LazySeq in cljs
  (is (= "[1,2,3]" (v/arg->str (map :x [{:x 1} {:x 2} {:x 3}])))))

(deftest arguments->str-test
  (is (= "" (v/arguments->str {})))
  (is (= "id:1" (v/arguments->str {:id 1})))
  (is (= "id:null" (v/arguments->str {:id nil})))
  (is (= "id:1,type:\"human\"" (v/arguments->str {:id 1 :type "human"})))
  (is (= "id:1,vector:[1,2,3]" (v/arguments->str {:id 1 :vector [1 2 3]}))))

(deftest meta-field->str
  (is (= "__typename" (v/meta-field->str :meta/typename))))

(deftest fields->str-test
  (is (= "name" (v/fields->str [[:venia/field :name]])))
  (is (= "name,address" (v/fields->str [[:venia/field :name] [:venia/field :address]])))
  (is (= "friends{name,email}" (v/fields->str [[:venia/nested-field {:venia/nested-field-root     :friends
                                                                     :venia/nested-field-children [[:venia/field :name]
                                                                                                   [:venia/field :email]]}]]))))

(deftest variables->str-test
  (is (= "$id:Int" (v/variables->str [{:variable/name "id"
                                       :variable/type [:type :Int]}])))
  (is (= "$ids:[Int!]" (v/variables->str [{:variable/name "ids"
                                           :variable/type [:list [:Int!]]}])))
  (is (= "$id:Int=2" (v/variables->str [{:variable/name    "id"
                                         :variable/type    [:type :Int]
                                         :variable/default 2}])))
  (is (= "$id:Int,$name:String" (v/variables->str [{:variable/name "id"
                                                    :variable/type [:type :Int]}
                                                   {:variable/name "name"
                                                    :variable/type [:type :String]}])))
  (is (= "$id:Int=1,$name:String=\"my-name\"" (v/variables->str [{:variable/name    "id"
                                                                  :variable/type    [:type :Int]
                                                                  :variable/default 1}
                                                                 {:variable/name    "name"
                                                                  :variable/type    [:type :String]
                                                                  :variable/default "my-name"}])))
  (is (= "" (v/variables->str nil)))
  (is (= "" (v/variables->str []))))

(deftest fragment->str-test
  (is (= "fragment comparisonFields on Worker{name,address,friends{name,email}}"
         (v/fragment->str {:fragment/name   "comparisonFields"
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

  (testing "Should create a valid graphql string with __typename meta field included"
    (let [data {:venia/queries [[:employee {:id 1 :active true} [:name :address :meta/typename [:friends [:meta/typename :name :email]]]]]}
          query-str "{employee(id:1,active:true){name,address,__typename,friends{__typename,name,email}}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string using params on nested fields that doesnt't have nested fields."
    (let [data {:venia/queries [[:employee {:id 1 :active true} [:name :address [:boss_name {:id 1}]]]]}
          query-str "{employee(id:1,active:true){name,address,boss_name(id:1)}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string using params on nested fields."
    (let [data {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends {:id 1} [:name :email]]]]]}
          query-str "{employee(id:1,active:true){name,address,friends(id:1){name,email}}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string using params on fields."
    (let [data {:venia/queries [[:employee [[:name {:isEmpty false}] :address [:friends [:name [:email {:isValid true}]]]]]]}
          query-str "{employee{name(isEmpty:false),address,friends{name,email(isValid:true)}}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string using params on different nested levels of fields."
    (let [data {:venia/queries [[:employee {:id 1 :active true} [[:name {:isEmpty false}] :address [:friends {:id 1} [:name [:email {:isValid true}]]]]]]}
          query-str "{employee(id:1,active:true){name(isEmpty:false),address,friends(id:1){name,email(isValid:true)}}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string when no args are required and no fields are specified."
    (let [data {:venia/queries [[:getDate]]}
          query-str "{getDate}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string there are args and no fields are specified."
    (let [data {:venia/queries [[:sayHello {:name "Tom"}]]}
          query-str "{sayHello(name:\"Tom\")}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string there are no args but fields are specified."
    (let [data {:venia/queries [[:sayHello [:name]]]}
          query-str "{sayHello{name}}"]
      (is (= query-str (v/graphql-query data)))))

  (testing "Invalid query, should throw exception"
    (is (thrown? #?(:clj  Exception
                    :cljs js/Error) (v/graphql-query []))))

  (testing "Should create a valid graphql string with query aliases"
    (let [data {:venia/queries [{:query/data  [:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]
                                 :query/alias :workhorse}
                                {:query/data  [:employee {:id 2 :active true} [:name :address [:friends [:name :email]]]]
                                 :query/alias :boss}]}
          query-str (str "{workhorse:employee(id:1,active:true){name,address,friends{name,email}},"
                         "boss:employee(id:2,active:true){name,address,friends{name,email}}}")]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql string with field aliases"
    (let [data {:venia/queries [[:employee {:id 1 :active true} [:name :address
                                                                 {:field/data [[:friends [:name :email]]]
                                                                  :field/alias :mates}
                                                                 {:field/data [[:friends [:name :email]]]
                                                                  :field/alias :enemies}]]]}
          query-str (str "{employee(id:1,active:true){name,address,mates:friends{name,email},enemies:friends{name,email}}}")]
      (is (= query-str (v/graphql-query data)))))

  (testing "Should create a valid graphql query with fragment"
    (let [data {:venia/queries   [{:query/data  [:employee {:id 1 :active true} :fragment/comparisonFields]
                                   :query/alias :workhorse}
                                  {:query/data  [:employee {:id 2 :active true} :fragment/comparisonFields]
                                   :query/alias :boss}]
                :venia/fragments [{:fragment/name   "comparisonFields"
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
                :venia/fragments [{:fragment/name   "comparisonFields"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name :address [:friends [:name :email]]]}
                                  {:fragment/name   "secondFragment"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name]}]}
          query-str (str "{workhorse:employee(id:1,active:true){...comparisonFields},boss:employee(id:2,active:true){...comparisonFields}} "
                         "fragment comparisonFields on Worker{name,address,friends{name,email}},"
                         "fragment secondFragment on Worker{name}")
          result (v/graphql-query data)]
      (is (= query-str result))))

  (testing "Should create a valid graphql query with multiple fragments for fields (deals with unions)"
    (let [data {:venia/queries   [{:query/data  [:employee {:id 1 :active true} [[:fragment/comparisonFields :fragment/secondFragment]] ]
                                   :query/alias :workhorse}
                                  {:query/data  [:employee {:id 2 :active true} :fragment/comparisonFields]
                                   :query/alias :boss}]
                :venia/fragments [{:fragment/name   "comparisonFields"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name :address [:friends [:name :email]]]}
                                  {:fragment/name   "secondFragment"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name]}]}
          query-str (str "{workhorse:employee(id:1,active:true){...comparisonFields ...secondFragment},boss:employee(id:2,active:true){...comparisonFields}} "
                         "fragment comparisonFields on Worker{name,address,friends{name,email}},"
                         "fragment secondFragment on Worker{name}")
          result (v/graphql-query data)]
      (is (= query-str result))))

  (testing "Should create a valid graphql query with nested fragments (deals with unions)"
    (let [data {:venia/queries   [{:query/data  [:employee {:id 1 :active true} [[:data [:fragment/comparisonFields :fragment/secondFragment]]] ]
                                   :query/alias :workhorse}
                                  {:query/data  [:employee {:id 2 :active true} :fragment/comparisonFields]
                                   :query/alias :boss}]
                :venia/fragments [{:fragment/name   "comparisonFields"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name :address [:friends [:name :email]]]}
                                  {:fragment/name   "secondFragment"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name]}]}
          query-str (str "{workhorse:employee(id:1,active:true){data{...comparisonFields ...secondFragment}},boss:employee(id:2,active:true){...comparisonFields}} "
                         "fragment comparisonFields on Worker{name,address,friends{name,email}},"
                         "fragment secondFragment on Worker{name}")
          result (v/graphql-query data)]
      (is (= query-str result))))

  (testing "Should create a valid graphql query with a fragment within a fragment field (deals with unions)"
    (let [data {:venia/queries   [{:query/data  [:employee {:id 1 :active true} :fragment/comparisonFields]
                                   :query/alias :workhorse}]
                :venia/fragments [{:fragment/name   "comparisonFields"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name :address [:friends [:name :email]] [:pet [:fragment/dog :fragment/cat]]]}
                                  {:fragment/name   "dog"
                                   :fragment/type   :Dog
                                   :fragment/fields [:name :bark]}
                                  {:fragment/name   "cat"
                                   :fragment/type   :Cat
                                   :fragment/fields [:name :purr]}]}
          query-str (str "{workhorse:employee(id:1,active:true){...comparisonFields}} "
                         "fragment comparisonFields on Worker{name,address,friends{name,email},pet{...dog ...cat}},"
                         "fragment dog on Dog{name,bark},"
                         "fragment cat on Cat{name,purr}")
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
                :venia/fragments [{:fragment/name   "comparisonFields"
                                   :fragment/type   :Worker
                                   :fragment/fields [:name :address [:friends [:name :email]]]}]}
          query-str (str "query employeeQuery($id:Int,$name:String){workhorse:employee(id:$id,active:true,name:$name){...comparisonFields},"
                         "boss:employee(id:$id,active:false){...comparisonFields}} fragment comparisonFields on Worker{name,address,friends{name,email}}")
          result (v/graphql-query data)]
      (is (= query-str result))))

  (testing "Should create a valid graphql mutation"
    (let [data {:venia/operation {:operation/type :mutation
                                  :operation/name "AddProjectToEmployee"}
                :venia/variables [{:variable/name "id"
                                   :variable/type :Int!}
                                  {:variable/name "project"
                                   :variable/type :ProjectNameInput!}]
                :venia/queries   [[:addProject {:employeeId :$id
                                                :project    :$project}
                                   [:allocation :name]]]}
          query-str (str "mutation AddProjectToEmployee($id:Int!,$project:ProjectNameInput!){addProject(employeeId:$id,project:$project){allocation,name}}")
          result (v/graphql-query data)]
      (is (= query-str result)))))

(deftest test-query-with-list-variables
  (is (= (v/graphql-query
          {:venia/operation
           {:operation/type :query
            :operation/name "MyQuery"}
           :venia/queries
           [[:node {:ids :$ids} [:id]]]
           :venia/variables
           [{:variable/name "ids"
             :variable/type [:ID!]}]})
         "query MyQuery($ids:[ID!]){node(ids:$ids){id}}")))
