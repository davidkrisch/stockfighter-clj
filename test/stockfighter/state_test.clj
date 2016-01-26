(ns stockfighter.state-test
  (:require [clojure.test :refer :all]
            [stockfighter.state :refer :all]))

; (test/run-tests 'stockfighter.state-test)

(def order {:open true
            :symbol "BML"
            :orderType "limit"
            :totalFilled 2
            :account "SAH10171568"
            :ts "2016-01-20T08:31:53.802834938Z"
            :id 3939
            :ok true
            :originalQty 100
            :venue "OYFVEX"
            :qty 2
            :fills
            [{:price 11472 :qty 2 :ts "2016-01-20T08:31:58.144995432Z"}]
            :price 11472
            :direction "sell"})

(def fill {:ok true
           :filled 2
           :order (assoc order :qty 2)})

(def sys (atom {:inventory 0
                :orders {3939 order}}))
(def empty-sys (atom (assoc @sys :orders {})))

(def expected {:inventory -2
               :orders {3939 (assoc order :qty 2)}})

(deftest handle-fill-tests
  (is (= (handle-fill empty-sys fill)
         expected))
  (is (= (handle-fill sys fill)
         expected)))

(deftest add-trade-tests
  (is (= (add-trade {:trades []} {:a 1})
         {:trades [{:a 1}]})))


(def by-id-sys (atom {:trades [{:response {:id 1}}
                                  {:response {:id 2}}
                                  {:response {:id 3}}]}))

(deftest by-id-tests
  (is (= (by-id by-id-sys 2)
         {:response {:id 2}}))
  (is (= (by-id by-id-sys 0)
         nil)))

(deftest index-of-tests
  (is (= (index-of by-id-sys 2) 1))
  (is (= (index-of by-id-sys 4) nil))
  (is (= (index-of by-id-sys nil) nil)))
