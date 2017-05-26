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
          (arg->str [arg] (str "\"" (name arg) "\""))
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
           (arg->str [arg] (str "\"" (name arg) "\""))
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
       (apply str)))

(defmulti ->query-str
  (fn [query]
    (cond (vector? query) (first query)
          (and (map? query) (:venia/query query)) :venia/query
          (and (map? query) (:venia/query-with-fragment query)) :venia/query-with-fragment
          (and (map? query) (:venia/fragment query)) :venia/fragment
          :else :default)))

(defmethod ->query-str :venia/query-vector
  [[_ query]]
  "Given a spec conformed query vector, creates query string with query, arguments and fields."
  (str "{"
       (->> (map ->query-str query)
            (interpose ",")
            (apply str))
       "}"))

(defmethod ->query-str :venia/query-map
  [[_ query]]
  "Given a spec conformed query map, creates query string with query, arguments and fields."
  (let [has-fragment? (-> query first :venia/fragment)
        wrapper-start (when-not has-fragment? "{")
        wrapper-end (when-not has-fragment? "}")]
    (str wrapper-start
         (->> (map ->query-str query)
              (interpose ",")
              (apply str))
         wrapper-end)))

(defmethod ->query-str :venia/query
  [query]
  "Processes a single query."
  (let [query-def (:venia/query query)
        alias (when (:venia/alias query) (str (name (:venia/alias query)) ":"))
        query-str (name (:query query-def))
        args (when (:args query-def) (str "(" (arguments->str (:args query-def)) ")"))
        fields (str "{" (fields->str (:fields query-def)) "}")]
    (str alias query-str args fields)))

(defmethod ->query-str :venia/query-with-fragment
  [query]
  "Processes a single query with defined fragment."
  (let [query-def (:venia/query-with-fragment query)
        alias (when (:venia/alias query) (str (name (:venia/alias query)) ":"))
        query-str (name (:query query-def))
        args (when (:args query-def) (str "(" (arguments->str (:args query-def)) ")"))
        fragment (str "{" "..." (name (:fragment-name query-def)) "}")]
    (str alias query-str args fragment)))

(defmethod ->query-str :venia/fragment
  [fragment]
  "Processes a single fragment definition."
  (let [fragment-def (:venia/fragment fragment)
        fields (str "{" (fields->str (:fragment/fields fragment-def)) "}")]
    (str "fragment "
         (name (:fragment/name fragment-def))
         " on "
         (name (:fragment/type fragment-def))
         fields)))

(defmethod ->query-str :default
  [query]
  "Processes simple query."
  (let [query-str (name (:query query))
        args (when (:args query) (str "(" (arguments->str (:args query)) ")"))
        fields (str "{" (fields->str (:fields query)) "}")]
    (str query-str args fields)))

(defn graphql-query
  "Formats clojure data structure to valid graphql query string."
  [data]
  (->> (map ->query-str (spec/query->spec data))
       (interpose " ")
       (apply str)))
