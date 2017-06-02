(ns venia.spec
  (:require #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
                    [venia.exception :as ex]))

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

(s/def :venia/query-name keyword?)
(s/def :venia/fields (s/conformer #(or-conformer % (s/or :fields (s/coll-of (s/or :venia/field keyword?
                                                                                  :venia/nested-field (s/cat :venia/nested-field-root keyword?
                                                                                                             :args (s/? :venia/args)
                                                                                                             :venia/nested-field-children :venia/fields)))
                                                         :fragment fragment-keyword?))))

(s/def :venia/args (s/keys :opt []))
(s/def :query/data (s/cat :query :venia/query-name :args (s/? :venia/args) :fields :venia/fields))
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

(s/def :venia/query-def (s/keys :req [:venia/queries]
                                :opt [:venia/fragments
                                      :venia/operation
                                      :venia/variables]))

(defn query->spec [query]
  (let [conformed (s/conform :venia/query-def query)]
    (if (= ::s/invalid conformed)
      (ex/throw-ex {:venia/ex-type    :venia/spec-validation
                    :venia/ex-explain (s/explain :venia/query-def query)})
      [:venia/query-def conformed])))
