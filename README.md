# venia [wip]

[![Build Status](https://travis-ci.org/Vincit/venia.svg?branch=master)](https://travis-ci.org/Vincit/venia)

A Clojure(Script) qraphql query client library. Generate valid graphql queries with Clojure data structures.

## Status

Venia is currently **WORK IN PROGRESS**. API is subject to change. However, contributions are warmly welcome.

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

(v/graphql-query [[[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]]])

=> "{employee(id:1,active:true){name,address,friends{name,email}}}"
```

Do not get shocked by the amount of nested vectors here, venia's structure is actually pretty simple:

```clj
[;; First level
  [;; Second level - collection of query definitions
    [;; Third level - query definiion
      :employee - object we are fetching
      {:id 1 :active true} - arguments
      [:name :address - fields
      [:friends [:name :email]]] - nested field with children fields
    ]
  ]
]
```

If we would like to fetch employees and projects within the same simple query, we would do it this way:

```clj
(v/graphql-query [;; First level
                   [;; Second level
                     [:employee {:active true} [:name :address]]
                     [:project {:active true} [:customer :price]]
                   ]
                 ])

=> "{employee(active:true){name,address},project(active:true){customer,price}}"
```

### Query with alias

Now, if we need to have an alias for query, it can be easily achieved by using venia's map query definition:

```clj
(v/graphql-query
  [[{:venia/query [:employee {:id 1 :active true} [:name :address]]
     :venia/alias :workhorse}
    {:venia/query [:employee {:id 2 :active true} [:name :address]]
     :venia/alias :boss}]])
     
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

In the query above, we use `:venia/query` key for query definition and `:venia/alias` for query's alias definition.

### Query with fragments

What about fragments? Venia supports them as well!

```clj
(v/graphqö-query [[{:venia/query-with-fragment [:employee
                                                {:id 1 :active true}
                                                :comparisonFields]
                    :venia/alias               :workhorse}
                   {:venia/query-with-fragment [:employee
                                                {:id 2 :active true}
                                                :comparisonFields]
                    :venia/alias               :boss}]
                  [{:venia/fragment {:fragment/name   :comparisonFields
                                     :fragment/type   :Worker
                                     :fragment/fields [:name :address]}}]])

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

This time, query is defined with `venia/query-with-fragment`, where we use a keyword instead of vector of fields.
Fragment itself is defined inside of separate from query definitions vector as `venia/fragment`.

## License

Copyright © 2017 Vincit

Distributed under the Eclipse Public License, the same as Clojure.