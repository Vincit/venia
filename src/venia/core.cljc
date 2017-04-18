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

(defn ->query-str
  "Given a spec conformed query map, creats query string with query, arguments and fields."
  [query]
  (let [query-str (name (:query query))
        args (when (:args query) (str "(" (arguments->str (:args query)) ")"))
        fields (str "{" (fields->str (:fields query)) "}")]
    (str query-str args fields)))

(defn graphql-query
  "Formats clojure data structure to valid graphql query string."
  [data]
  (str "{"
       (->>
         (map ->query-str (spec/query->spec data))
         (interpose ",")
         (apply str))
       "}"))