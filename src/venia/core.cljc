(ns venia.core
  (:require [venia.spec :as spec]
            [clojure.string :as str])
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

(defn sequential->str
  "Given something that is sequential format it to be like a JSON array."
  [arg]
  (str "[" (apply str (interpose "," (map arg->str arg))) "]"))

(defn encode-string
  "Handles escaping special characters."
  [arg]
  (str "\"" (str/escape arg {\"         "\\\""
                             \\         "\\\\"
                             \newline   "\\n"
                             \return    "\\r"
                             \tab       "\\t"
                             \formfeed  "\\f"
                             \backspace "\\b"}) "\""))

#?(:clj (extend-protocol ArgumentFormatter
          nil
          (arg->str [arg] "null")
          String
          (arg->str [arg] (encode-string arg))
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
           (arg->str [arg] (encode-string arg))
           PersistentArrayMap
           (arg->str [arg] (str "{" (arguments->str arg) "}"))
           PersistentHashMap
           (arg->str [arg] (str "{" (arguments->str arg) "}"))
           PersistentVector
           (arg->str [arg] (sequential->str arg))
           IndexedSeq
           (arg->str [arg] (sequential->str arg))
           LazySeq
           (arg->str [arg] (sequential->str arg))
           List
           (arg->str [arg] (sequential->str arg))
           Keyword
           (arg->str [arg] (name arg))
           number
           (arg->str [arg] (str arg))
           object
           (arg->str [arg] (str arg))
           boolean
           (arg->str [arg] (str arg))))

(defn meta-field->str
  "Converts namespaced meta field keyword to graphql format, e.g :meta/typename -> __typename"
  [meta-field]
  (str "__" (name meta-field)))

(defn fields->str
  "Given a spec conformed vector of query fields (and possibly nested fields),
  concatenates them to string, keeping nested structures intact."
  [fields]
  (if (keyword? fields)
    (str "..." (name fields))
    (->> (for [[type value] fields]
           (condp = type
             :venia/meta-field (meta-field->str value)
             :venia/field (name value)
             :venia/field-with-args (str (name (:venia/field value))
                                         (when (:args value)
                                           (str "(" (arguments->str (:args value)) ")")))
             :venia/field-with-data (str (when-let [alias (name (:field/alias value))]
                                           (str alias ":"))
                                         (fields->str (:field/data value)))
             :venia/nested-field (str (name (:venia/nested-field-root value))
                                      (when (:args value)
                                        (str "(" (arguments->str (:args value)) ")"))
                                      "{"
                                      (fields->str (:venia/nested-field-children value))
                                      "}")
             :venia/nested-field-arg-only (str (name (:venia/nested-field-root value))
                                               (str "(" (arguments->str (:args value)) ")"))
             :venia/fragments (str/join " " (map #(str "..." (name %)) value))
             :venia/nested-field-with-fragments (str (name (:venia/nested-field-root value))
                                                     "{"
                                                     (str/join " " (map #(str "..." (name %))
                                                                        (:venia/fragments value)))
                                                     "}")))
         (interpose ",")
         (apply str))))

(defn variables->str
  "Given a vector of variable maps, formats them and concatenates to string.

  E.g. (variables->str [{:variable/name \"id\" :variable/type :Int}]) => \"$id: Int\""
  [variables]
  (->> (for [{var-name :variable/name var-type :variable/type var-default :variable/default} variables]
         (str "$" var-name ":" (name var-type) (when var-default (str "=" (arg->str var-default)))))
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

(defn include-fields?
  "Include fields if fields is not empty or is a keyword.
   fields could be nil or empty for operations that return a scalar."
  [fields]
  (or (keyword? fields)
      (not (empty? fields))))

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
        operation-with-name (when operation (str (name (:operation/type operation)) " " (:operation/name operation)))
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
        fields (when (include-fields? (:fields query)) (str "{" (fields->str (:fields query)) "}"))]
    (str query-str args fields)))

(defmethod ->query-str :default
  [query]
  "Processes a query map (with query name, args and fields)"
  (let [query-str (name (:query query))
        args (when (:args query) (str "(" (arguments->str (:args query)) ")"))
        fields (when (include-fields? (:fields query)) (str "{" (fields->str (:fields query)) "}"))]
    (str query-str args fields)))

(defn graphql-query
  "Formats clojure data structure to valid graphql query string."
  [data]
  (-> (spec/query->spec data)
      ->query-str))
