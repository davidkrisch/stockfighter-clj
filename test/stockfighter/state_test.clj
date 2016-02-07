(ns stockfighter.state-test
  (:require [clojure.test :refer :all]
            [manifold.deferred :as d]
            [cheshire.core :as json]
            [byte-streams :as bs]
            [clojure.tools.logging :as log]
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


(defn mock-response [id]
  (let [ddd (d/deferred)
        b (json/generate-string {:id id})]
    (d/success! ddd {:body (bs/to-input-stream b)})
    ddd))

(def response1 (mock-response 1))
(def response2 (mock-response 2))
(def response3 (mock-response 3))

(def by-id-sys {:trades [{:internal-id (uuid)
                          :response response1}
                         {:internal-id (uuid)
                          :response response2}
                         {:internal-id (uuid)
                          :response response3}]})

;(deftest by-id-tests
  ;(is (= (by-id by-id-sys 2)
         ;{:response response2}))
  ;(is (= (by-id by-id-sys 0)
         ;nil)))

(defn second-id [sys]
  (-> :trades
      sys
      (nth 1)
      :internal-id))

(deftest index-of-tests
  (let [trades (:trades by-id-sys)]
    (is (= (index-of trades (second-id by-id-sys)) 1))
    (is (= (index-of trades 0) nil))
    (is (= (index-of trades nil) nil))))

(deftest update-trade-tests
  (let [update {:the "update" :foo "bar"}]
    (is (= (update-trade by-id-sys
                         (second-id by-id-sys)
                         update)
           (assoc-in by-id-sys [:trades 1 :status] update)))))

;
; Test position & should-trade?
;

(defn mock-trades [bought sold]
  (atom {:trades [{:status {:open true
                            :totalFilled bought
                            :price 10
                            :direction "buy"}
                   :request-body {:direction "buy"
                                  :qty 10}}
                  {:status nil
                   :request-body {:direction "sell"
                                  :qty 10}}
                  {:status {:open false
                            :totalFilled 10
                            :price 11
                            :direction "sell"}
                   :request-body {:direction "sell"
                                  :qty 10}}
                  {:status nil
                   :request-body {:direction "sell"
                                  :qty 20}}]}))


(deftest position-tests
  (is (= (position (mock-trades 10 10)) {:shares 0 :cash 10}))
  (is (= (position (mock-trades 8 10)) {:shares -2 :cash 30})))


(deftest should-trade?-tests
  (is (= (should-trade? (mock-trades 0 0) "buy")
         {:ok true :qty 10}))
  (is (= (should-trade? (mock-trades 249 10) "buy")
         {:ok true :qty 10}))
  (is (= (should-trade? (mock-trades 250 0) "buy")
         {:ok false}))
  (is (= (should-trade? (mock-trades 0 0) "sell")
         {:ok true :qty 10}))
  (is (= (should-trade? (mock-trades 10 259) "sell")
         {:ok true :qty 10}))
  (is (= (should-trade? (mock-trades 0 260) "sell")
         {:ok false})))


(deftest outstanding-tests
  (is (= (outstanding (mock-trades 0 0)) 10)))
