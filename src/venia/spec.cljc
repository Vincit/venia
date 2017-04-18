(ns venia.spec
  (:require #?(:clj [clojure.spec :as s]
               :cljs [cljs.spec :as s])))

(s/def :venia/query-name keyword?)

(s/def :venia/fields (s/coll-of (s/or :venia/field keyword?
                                      :venia/nested-field (s/cat :venia/nested-field-root keyword? :venia/nested-field-children :venia/fields))))

(s/def :venia/args (s/keys :opt-un [:venia/alias]))

(s/def :venia/query-def (s/cat :query :venia/query-name :args (s/? :venia/args) :fields :venia/fields))
(s/def :venia/alias keyword?)

(s/def :venia/query-with-meta (s/keys :req [:venia/query-def]
                                      :opt [:venia/alias]))

(s/def :venia/query (s/coll-of :venia/query-def))

(defn query->spec [query]
  (s/conform :venia/query query))