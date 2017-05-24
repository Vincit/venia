(ns venia.exception)

(defmulti throw-ex :venia/ex-type)

#?(:clj (defmethod throw-ex :venia/spec-validation
          [data]
          (throw (ex-info "Invalid query data" data))))

#?(:cljs (defmethod throw-ex :venia/spec-validation
           [data]
           (throw (js/Error. (str "Invalid query data " data)))))
