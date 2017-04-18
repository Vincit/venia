(ns venia.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [venia.core-test]))

(enable-console-print!)

(doo-tests 'venia.core-test)