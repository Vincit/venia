(ns venia.core-test
  (:require [venia.core :as v]
    #?(:cljs [cljs.test :refer-macros [is are deftest testing]]
       :clj
            [clojure.test :refer :all])))

(deftest ArgumentFormatter-test
  (is (= "" (v/arg->str nil)))
  (is (= "\"human\"" (v/arg->str "human")))
  (is (= "{id:1}" (v/arg->str {:id 1})))
  (is (= "[1,2,3]" (v/arg->str [1 2 3])))
  (is (= "[1,{id:1},\"human\"]" (v/arg->str [1 {:id 1} "human"])))
  (is (= "\"human\"" (v/arg->str :human)))
  (is (= "1" (v/arg->str 1)))
  (is (= "{active:true}" (v/arg->str {:active true}))))

(deftest arguments->str-test
  (is (= "" (v/arguments->str {})))
  (is (= "id:1" (v/arguments->str {:id 1})))
  (is (= "id:1,type:\"human\"" (v/arguments->str {:id 1 :type "human"})))
  (is (= "id:1,vector:[1,2,3]" (v/arguments->str {:id 1 :vector [1 2 3]}))))

(deftest fields->str-test
  (is (= "name" (v/fields->str [[:venia/field :name]])))
  (is (= "name,address" (v/fields->str [[:venia/field :name] [:venia/field :address]])))
  (is (= "friends{name,email}" (v/fields->str [[:venia/nested-field {:venia/nested-field-root     :friends
                                                                     :venia/nested-field-children [[:venia/field :name]
                                                                                                   [:venia/field :email]]}]]))))

(deftest graphql-query-test
  (testing "Should create a valid graphql string."
    (let [data [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]
          query-str "{employee(id:1,active:true){name,address,friends{name,email}}}"]
      (is (= query-str (v/graphql-query data))))))
