(ns venia.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [venia.core-test]
            [venia.spec-test]))

(enable-console-print!)

(doo-tests 'venia.core-test
           'venia.spec-test)