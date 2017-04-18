(ns venia.core
  (:require [venia.spec :as spec])
  #?(:clj
     (:import (clojure.lang IPersistentMap Keyword IPersistentCollection))))

(defprotocol ArgumentFormatter
  (arg->str [arg]))

(defn arguments->str [args]
  (->> (for [[k v] args]
         [(name k) ":" (arg->str v)])
       (interpose ",")
       flatten
       (apply str)))

#?(:clj (extend-protocol ArgumentFormatter
          nil
          (arg->str [arg] "")
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
           (arg->str [arg] "")
           js/String
           (arg->str [arg] (str "\"" arg "\""))
           PersistentArrayMap
           (arg->str [arg] (str "{" (arguments->str arg) "}"))
           PersistentVector
           (arg->str [arg] (str "[" (apply str (interpose "," (map arg->str arg))) "]"))
           Keyword
           (arg->str [arg] (str "\"" (name arg) "\""))
           js/Object
           (arg->str [arg] (str arg))))

(defn fields->str [fields]
  (->> (for [[type value] fields]
         (condp = type
           :venia/field (name value)
           :venia/nested-field (str (name (:venia/nested-field-root value)) "{" (fields->str (:venia/nested-field-children value)) "}")))
       (interpose ",")
       (apply str)))

(defmulti ->query-str (fn [query] (cond (vector? query) (first query)
                                        (and (map? query)
                                             (:venia/query-def query)) :venia/query-def
                                        :else :default)))

(defmethod ->query-str :venia/query-vector
  [[_ query]]
  "Given a spec conformed query map, creats query string with query, arguments and fields."
  (str "{"
       (->> (map ->query-str query)
            (interpose ",")
            (apply str))
       "}"))

(defmethod ->query-str :venia/query-with-meta
  [[_ query]]
  (str "{"
       (->> (map ->query-str query)
            (interpose ",")
            (apply str))
       "}"))

(defmethod ->query-str :venia/query-def
  [query]
  (let [query-def (:venia/query-def query)
        alias (when (:venia/alias query) (str (name (:venia/alias query)) ":"))
        query-str (name (:query query-def))
        args (when (:args query-def) (str "(" (arguments->str (:args query-def)) ")"))
        fields (str "{" (fields->str (:fields query-def)) "}")]
    (str alias query-str args fields)))

(defmethod ->query-str :default
  [query]
  (let [query-str (name (:query query))
        args (when (:args query) (str "(" (arguments->str (:args query)) ")"))
        fields (str "{" (fields->str (:fields query)) "}")]
    (str query-str args fields)))

(defn graphql-query
  "Formats clojure data structure to valid graphql query string."
  [data]
  (->query-str (spec/query->spec data)))