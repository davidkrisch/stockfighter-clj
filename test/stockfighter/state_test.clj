(ns stockfighter.state-test
  (:require [clojure.test :refer :all]
            [stockfighter.state :refer :all]))

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
