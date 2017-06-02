# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.2.1 - 2017-06-02
### Added 
- Support for variables

## Changed
**Breaking changes:**
- Large refactor of core namespace - API changes

Moved away from huge nested vectors as query definitions to neat query map definitions. After all, simplicity is all that matters.
Now, queries are defined like this:
```clj
{:venia/operation {:operation/type :query
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
```

`:venia/queries` is only one required key, others are optional. However, rest of data needs to be valid as well if added.

Query's data can be also defined as simple vector:

```clj
{:venia/queries [[:employee {:id 1 :active true} [:name :address [:friends {:id 1} [:name :email]]]]]}
```
- Fragments functionality updated to be more explicit

Notice how fragments are defined and referenced in code example above. 


## 0.1.2 - 2017-05-30
### Changed
- Updated clojure and clojurescript to 1.9.0-alpha17 and 1.9.562

### Fixed
- 'nil' argument is serialized to 'null'

### Added
- Docstrings to functions in core namespace

## 0.1.1 - 2017-05-25
### Fixed
- Fix ArgumentFormatter in cljs
- Fux throw-ex in cljs

### Added
- Nested fields can also have arguments

## 0.1.0 - 2017-04-30
Initial release