(ns venia.core
  (:require [venia.spec :as spec])
  #?(:clj
     (:import (clojure.lang IPersistentMap Keyword IPersistentCollection))))

(defprotocol ArgumentFormatter
  "Protocol responsible for query arguments' formatting to string.
  Has separate implementations for general data types in cljs and clj."
  (arg->str [arg]))

(defn arguments->str
  "Given a map of query arguments, formats them and concatenates to string.

  E.g. (arguments->str {:id 1 :type \"human\"}) => id:1,type:\"human\""
  [args]
  (->> (for [[k v] args]
         [(name k) ":" (arg->str v)])
       (interpose ",")
       flatten
       (apply str)))

#?(:clj (extend-protocol ArgumentFormatter
          nil
          (arg->str [arg] "null")
          String
          (arg->str [arg] (str "\"" arg "\""))
          IPersistentMap
          (arg->str [arg] (str "{" (arguments->str arg) "}"))
          IPersistentCollection
          (arg->str [arg] (str "[" (apply str (interpose "," (map arg->str arg))) "]"))
          Keyword
          (arg->str [arg] (name arg))
          Object
          (arg->str [arg] (str arg))))

#?(:cljs (extend-protocol ArgumentFormatter
           nil
           (arg->str [arg] "null")
           string
           (arg->str [arg] (str "\"" arg "\""))
           PersistentArrayMap
           (arg->str [arg] (str "{" (arguments->str arg) "}"))
           PersistentVector
           (arg->str [arg] (str "[" (apply str (interpose "," (map arg->str arg))) "]"))
           Keyword
           (arg->str [arg] (name arg))
           number
           (arg->str [arg] (str arg))
           object
           (arg->str [arg] (str arg))
           boolean
           (arg->str [arg] (str arg))))

(defn fields->str
  "Given a spec conformed vector of query fields (and possibly nested fields),
  concatenates them to string, keeping nested structures intact."
  [fields]
  (if (keyword? fields)
    (str "..." (name fields))
    (->> (for [[type value] fields]
           (condp = type
             :venia/field (name value)
             :venia/nested-field (str (name (:venia/nested-field-root value))
                                      (when (:args value)
                                        (str "(" (arguments->str (:args value)) ")"))
                                      "{"
                                      (fields->str (:venia/nested-field-children value))
                                      "}")))
         (interpose ",")
         (apply str))))

(defn variables->str
  "Given a vector of variable maps, formats them and concatenates to string.

  E.g. (variables->str [{:variable/name \"id\" :variable/type :Int}]) => \"$id: Int\""
  [variables]
  (->> (for [{var-name :variable/name var-type :variable/type} variables]
         (str "$" var-name ":" (name var-type)))
       (interpose ",")
       (apply str)))

(defn fragment->str
  "Given a fragment map, formats it and concatenates to string,"
  [fragment]
  (let [fields (str "{" (fields->str (:fragment/fields fragment)) "}")]
    (str "fragment "
         (name (:fragment/name fragment))
         " on "
         (name (:fragment/type fragment))
         fields)))

(defmulti ->query-str
  (fn [query]
    (cond (vector? query) (first query)
          (:venia/query query) :venia/query
          (:venia/query-with-data query) :venia/query-with-data
          :else :default)))

(defmethod ->query-str :venia/query-vector
  [[_ query]]
  "Given a spec conformed query vector, creates query string with query, arguments and fields."
  (str "{"
       (->> (map ->query-str query)
            (interpose ",")
            (apply str))
       "}"))

(defmethod ->query-str :venia/query-def
  [[_ query]]
  "Given a spec conformed root query map, creates a complete query string."
  (let [operation (:venia/operation query)
        operation-name (:venia/operation-name query)
        operation-with-name (when (and operation operation-name) (str (name operation) " " operation-name))
        variables (:venia/variables query)
        variables-str (when variables (str "(" (variables->str variables) ")"))
        fragments (:venia/fragments query)
        fragments-str (when fragments (str " " (->> (map fragment->str fragments)
                                                    (interpose ",")
                                                    (apply str))))]
    (str operation-with-name
         variables-str
         "{"
         (->> (map ->query-str (:venia/queries query))
              (interpose ",")
              (apply str))
         "}"
         fragments-str)))

(defmethod ->query-str :venia/query
  [query]
  "Processes a single query."
  (let [query-def (:venia/query query)
        alias (when (:query/alias query) (str (name (:query/alias query)) ":"))
        query-str (name (:query query-def))
        args (when (:args query-def) (str "(" (arguments->str (:args query-def)) ")"))
        fields (str "{" (fields->str (:fields query-def)) "}")]
    (str alias query-str args fields)))

(defmethod ->query-str :venia/queries
  [[_ query]]
  (str "{"
       (->> (map ->query-str query)
            (interpose ",")
            (apply str))
       "}"))

(defmethod ->query-str :venia/query-with-data
  [[_ query]]
  (let [query-str (->query-str (:query/data query))
        alias (when (:query/alias query) (str (name (:query/alias query)) ":"))]
    (str alias query-str)))

(defmethod ->query-str :query/data
  [[_ query]]
  "Processes simple query."
  (let [query-str (name (:query query))
        args (when (:args query) (str "(" (arguments->str (:args query)) ")"))
        fields (str "{" (fields->str (:fields query)) "}")]
    (str query-str args fields)))

(defmethod ->query-str :default
  [query]
  "Processes a query map (with query name, args and fields)"
  (let [query-str (name (:query query))
        args (when (:args query) (str "(" (arguments->str (:args query)) ")"))
        fields (str "{" (fields->str (:fields query)) "}")]
    (str query-str args fields)))

(defn graphql-query
  "Formats clojure data structure to valid graphql query string."
  [data]
  (-> (spec/query->spec data)
      ->query-str))
