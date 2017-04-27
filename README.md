# venia

[![Build Status](https://travis-ci.org/Vincit/venia.svg?branch=master)](https://travis-ci.org/Vincit/venia)

A Clojure(Script) qraphql query client library. Generate valid graphql queries with Clojure data structures.

## Usage

```clj
(ns my.project
  (:require [venia.core :as v]))

(v/graphql-query [[:employee {:id 1 :active true} [:name :address [:friends [:name :email]]]]])

=> "{employee(id:1,active:true){name,address,friends{name,email}}}"
```

## License

Copyright © 2017 Vincit

Distributed under the Eclipse Public License, the same as Clojure.