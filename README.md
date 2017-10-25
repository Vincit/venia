# venia


[![Clojars Project](https://img.shields.io/clojars/v/vincit/venia.svg)](https://clojars.org/vincit/venia)


[![Build Status](https://travis-ci.org/Vincit/venia.svg?branch=master)](https://travis-ci.org/Vincit/venia)

A Clojure(Script) qraphql query client library. Generate valid graphql queries with Clojure data structures.

## Usage

Venia is originally supposed to be used in Clojurescript apps, but can be used as well in Clojure, as the core 
is written in CLJC. The sole purpose of this library is graphql query string generation from Clojure data, 
so that strings concatenations and manipulations could be avoided when using grapqhl.
It is up to developers to hook it up to frontend apps. However, at least some sort of re-frame-grapqhl-fx library 
is on a roadmap. 


### Simple query

The easiest way to start with venia, is simple's query generation. 

```clj
(ns my.project
  (:require [venia.core :as v]))

(v/graphql-query {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]})

=> "{employee(id:1,active:true){name,address,friends{name,email}}}"
```

Obviously, If we would like to fetch employees and projects within the same simple query, we would do it this way:

```clj
(v/graphql-query {:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]
                                  [:projects {:active true} [:customer :price]]]})

=> "{employee(active:true){name,address},project(active:true){customer,price}}"
```

### Field arguments

In the example above, `:employee` and `:projects` fields have arguments `{:id 1 :active true}` and `{:id 1 :active true}` 
respectively.

We can add arguments to other fields easily by wrapping field name and its arguments to vector `[:customer {:id 2}]`:

```clj
(v/graphql-query {:venia/queries [[:projects {:active true} [[:customer {:id 2}] :price]]]})

=> "{project(active:true){customer(id:2),price}}"
```

### Query with alias

Now, if we need to have an alias for query, it can be easily achieved by using venia's query-with-data map

```clj
(v/graphql-query {:venia/queries [{:query/data [:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]
                                   :query/alias :workhorse}
                                  {:query/data  [:employee {:id 2 :active true} [:name :address [:friends [:name :email]]]]
                                   :query/alias :boss}]})
     
=> prettified:
{
  workhorse: employee(id: 1, active: true) {
    name
    address
  },
  boss: employee(id: 2, active: true) {
    name
    address
  }
}
```

In the query above, we use `:query/data` key for query definition and `:query/alias` for query's alias definition.

### Query with fragments

What about fragments? Just add `:venia/fragments` vector with fragments definitions

```clj
(v/graphql-query {:venia/queries   [{:query/data  [:employee {:id 1 :active true} :fragment/comparisonFields]
                                     :query/alias :workhorse}
                                    {:query/data  [:employee {:id 2 :active true} :fragment/comparisonFields]
                                     :query/alias :boss}]
                  :venia/fragments [{:fragment/name   "comparisonFields"
                                     :fragment/type   :Worker
                                     :fragment/fields [:name :address]}]})

=> prettified:
{
  workhorse: employee(id: 1, active: true) {
    ...comparisonFields
  }
  boss: employee(id: 2, active: true) {
    ...comparisonFields
  }
}

fragment comparisonFields on Worker {
  name
  address
}
```

### Query with variables

Now you can generate really complex queries with variables as well. In order to define variables, we need to define 
an operation type and name.


```clj
(v/graphql-query {:venia/operation {:operation/type :query
                                    :operation/name "employeeQuery"}
                  :venia/variables [{:variable/name    "id"
                                     :variable/type    :Int
                                     :variable/default 1}
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
                                     :fragment/fields [:name :address [:friends [:name :email]]]}]})

=> prettified:
query employeeQuery($id: Int = 1, $name: String) {
  workhorse: employee(id: $id, active: true, name: $name) {
    ...comparisonFields
  }
  boss: employee(id: $id, active: false) {
    ...comparisonFields
  }
}

fragment comparisonFields on Worker {
  name
  address
  friends {
    name
    email
  }
}

```

### Mutation

Mutations are also supported, just use `:mutation` operation type:

```clj

(v/graphql-query {:venia/operation {:operation/type :mutation
                                    :operation/name "AddProjectToEmployee"}
                  :venia/variables [{:variable/name "id"
                                     :variable/type :Int!}
                                    {:variable/name "project"
                                     :variable/type :ProjectNameInput!}]
                  :venia/queries   [[:addProject {:employeeId :$id
                                                  :project    :$project}
                                     [:allocation :name]]]})
                                     
=> prettified:
mutation AddProjectToEmployee($id:Int!,$project:ProjectNameInput!) {
  addProject(employeeId:$id, project:$project) {
    allocation,
    name
  }
}
```

### Validation

Venia will verify that you don't use undefined variables or fragments. 

For example, the following `v/graphql-query` calls will throw exceptions:

```clj

(v/graphql-query {:venia/queries [[:employee {:id 1 :active true} :fragment/undefined]]}

(v/graphql-query {:venia/queries [[:employee {:id 1 :active :$undefined} [:name]]]}))
```

because fragment and variable are never defined.

### Meta fields

You can use graphql's `__typename` meta field anywhere inside of your query.
For example:

```clj
(v/graphql-query {:venia/queries [[:employee [:meta/typename :name :address]]}

=> prettified:

{
  employee {
    __typename,
    name,
    address
  }
}

```


## License

Copyright © 2017 Vincit

Distributed under the Eclipse Public License, the same as Clojure.