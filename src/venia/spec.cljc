(ns venia.spec
  (:require #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
                    [venia.exception :as ex]
                    [clojure.set :as c-set]
                    [clojure.string :as c-string]))

(defn- fragment-keyword?
  "Checks if keyword has :fragment namespace"
  [x]
  (if (and (keyword? x)
           (= "fragment" (namespace x)))
    x
    ::s/invalid))

(defn- or-conformer
  "Conforms x and returns only conformed value without value type."
  [x spec]
  (second (s/conform spec x)))

(defn- extract-fragments-name [query]
  (let [fields (or (get-in query [:query/data :fields]) (:fields query))]
    (if (keyword? fields)
      (name fields)
      nil)))

(defn- resolve-used-fragments
  [x]
  (->> x
       :venia/queries
       (map #(-> %
                 second
                 extract-fragments-name))
       (remove nil?)
       set))

(defn- valid-fragments
  "Checks that all fragments used in queries are actually defined."
  [x]
  (if-not (:venia/fragments x)
    (let [used-fragments (resolve-used-fragments x)]
      (if-not (empty? used-fragments)
        (ex/throw-ex {:venia/ex-type :venia/invalid-fragments
                      :venia/ex-data used-fragments})
        x))

    (let [fragment-names (->> x
                              :venia/fragments
                              (map :fragment/name)
                              set)
          used-fragments (resolve-used-fragments x)
          undefined-fragments (c-set/difference used-fragments fragment-names)]
      (if (empty? undefined-fragments)
        x
        (ex/throw-ex {:venia/ex-type :venia/invalid-fragments
                      :venia/ex-data undefined-fragments})))))

(defn- extract-variables [query]
  (let [args (or (get-in query [:query/data :args]) (:args query))]
    (->> args
         vals
         (filter #(and (keyword? %)
                       (c-string/index-of (name %) "$")))
         (map #(c-string/replace (name %) "$" "")))))

(defn- resolve-used-variables
  [x]
  (->> x
       :venia/queries
       (map #(-> %
                 second
                 extract-variables))
       flatten
       (remove nil?)
       set))

(defn- valid-variables
  "Checks that all variables used in queries are actually defined."
  [x]
  (if-not (:venia/variables x)
    (let [used-variables (resolve-used-variables x)]
      (if-not (empty? used-variables)
        (ex/throw-ex {:venia/ex-type :venia/invalid-variables
                      :venia/ex-data used-variables})
        x))

    (let [variables-names (->> x
                               :venia/variables
                               (map #(c-string/replace (:variable/name %) "$" ""))
                               set)
          used-variables (resolve-used-variables x)
          undefined-variables (c-set/difference used-variables variables-names)]
      (if (empty? undefined-variables)
        x
        (ex/throw-ex {:venia/ex-type :venia/invalid-variables
                      :venia/ex-data undefined-variables})))))

(s/def :venia/query-name keyword?)
(s/def :venia/fields
  (s/conformer
    #(or-conformer %
                   (s/or :fields
                         (s/coll-of (s/or :venia/field keyword?
                                          :venia/nested-field-arg-only (s/cat :venia/nested-field-root keyword?
                                                                              :args :venia/args)
                                          :venia/nested-field (s/cat :venia/nested-field-root keyword?
                                                                     :args (s/? :venia/args)
                                                                     :venia/nested-field-children :venia/fields)))
                         :fragment fragment-keyword?))))

(s/def :venia/args (s/keys :opt []))
(s/def :query/data (s/cat :query :venia/query-name :args (s/? :venia/args) :fields (s/? :venia/fields)))
(s/def :venia/query (s/or :query/data :query/data
                          :venia/query-with-data (s/keys :req [:query/data]
                                                         :opt [:query/alias])))
(s/def :query/alias keyword?)

(s/def :fragment/name string?)
(s/def :fragment/type keyword?)
(s/def :fragment/fields :venia/fields)
(s/def :venia/fragment (s/keys :req [:fragment/name :fragment/type :fragment/fields]))
(s/def :venia/fragments (s/coll-of :venia/fragment :min-count 1))

(s/def :operation/type #{:query})
(s/def :operation/name string?)
(s/def :venia/operation (s/keys :req [:operation/type :operation/name]))

(s/def :variable/name string?)
(s/def :variable/type keyword?)
(s/def :query/variable (s/keys :req [:variable/name :variable/type]))
(s/def :venia/variables (s/coll-of :query/variable :min-count 1))

(s/def :venia/queries (s/coll-of :venia/query :min-count 1))


(s/def :venia/valid-fragments (s/conformer valid-fragments))
(s/def :venia/valid-variables (s/conformer valid-variables))

(s/def :venia/query-def (s/and (s/keys :req [:venia/queries]
                                       :opt [:venia/fragments
                                             :venia/operation
                                             :venia/variables])
                               :venia/valid-fragments
                               :venia/valid-variables))

(defn query->spec [query]
  (let [conformed (s/conform :venia/query-def query)]
    (if (= ::s/invalid conformed)
      (ex/throw-ex {:venia/ex-type    :venia/spec-validation
                    :venia/ex-explain (s/explain :venia/query-def query)})
      [:venia/query-def conformed])))
