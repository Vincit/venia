(ns venia.exception)

(defmulti throw-ex :venia/ex-type)

#?(:clj (defmethod throw-ex :venia/spec-validation
          [data]
          (throw (ex-info "Invalid query data" data))))

#?(:cljs (defmethod throw-ex :venia/spec-validation
           [data]
           (throw (js/Error. (str "Invalid query data " data)))))

#?(:clj (defmethod throw-ex :venia/invalid-fragments
          [data]
          (throw (ex-info (str "Invalid fragments: " (:venia/ex-data data)) data))))

#?(:cljs (defmethod throw-ex :venia/invalid-fragments
           [data]
           (throw (js/Error. (str "Invalid fragments: " (:venia/ex-data data))))))

#?(:clj (defmethod throw-ex :venia/invalid-variables
          [data]
          (throw (ex-info (str "Invalid variables: " (:venia/ex-data data)) data))))

#?(:cljs (defmethod throw-ex :venia/invalid-variables
           [data]
           (throw (js/Error. (str "Invalid variables: " (:venia/ex-data data))))))
