(ns stockfighter.level2
  (:require [stockfighter.client :as client :refer :all]))

;; ----------------------------
;; Level 2 - Buy 100,000 shares
;; ----------------------------

(def totalFilled (atom 0))

(defn level2 [account venue stock price]
  (while (< @totalFilled 100000)
    (let [request-body (order-body account venue stock (rand-int 1000) price "buy" "immediate-or-cancel")
          response-body (body (order venue stock request-body))]
      (prn " ------> " request-body)
      (prn " ---------> " response-body)
      (if (contains? response-body "totalFilled")
        (let [filled (response-body "totalFilled")]
          (prn "********---> filled: " filled)
          (swap! totalFilled + filled))))))
