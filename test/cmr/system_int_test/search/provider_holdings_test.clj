(ns cmr.system-int-test.search.provider-holdings-test
  "Integration tests for provider holdings"
  (:require [clojure.test :refer :all]
            [cmr.system-int-test.utils.ingest-util :as ingest]
            [cmr.system-int-test.utils.search-util :as search]
            [cmr.system-int-test.utils.index-util :as index]
            [cmr.system-int-test.data2.collection :as dc]
            [cmr.system-int-test.data2.granule :as dg]
            [cmr.system-int-test.data2.core :as d]))

;; total number of collections in PROV1
(def prov1-collection-count 5)

;; number of granule increment in each collection,
;; e.g. first collection has prov1-grans-increment-count number of granules
;; second collection has (2 * prov1-grans-increment-count) number of granules
;; third collection has (3 * prov1-grans-increment-count) number of granules and so on.
(def prov1-grans-increment-count 2)
(def prov2-collection-count 3)
(def prov2-grans-increment-count 3)

(defn- collection-holding
  "Returns the collection holding for the given collection and granule count"
  [provider-id coll granule-count]
  (let [{:keys [entry-title concept-id]} coll]
    {:entry-title entry-title
     :concept-id concept-id
     :granule-count granule-count
     :provider-id provider-id}))

(defn create-holdings
  "Set up the provider holdings fixtures for tests."
  []
  (let [prov1-colls (for [_ (range 0 prov1-collection-count)]
                      (d/ingest "PROV1" (dc/collection)))
        prov1-granule-counts (map #(* prov1-grans-increment-count %) (range 1 (inc prov1-collection-count)))
        prov1-holdings (map (partial collection-holding "PROV1") prov1-colls prov1-granule-counts)
        prov2-colls (for [_ (range 0 prov2-collection-count)]
                      (d/ingest "PROV2" (dc/collection)))
        prov2-granule-counts (map #(* prov2-grans-increment-count %) (range 1 (inc prov2-collection-count)))
        prov2-holdings (map (partial collection-holding "PROV2") prov2-colls prov2-granule-counts)]
    (dotimes [n prov1-collection-count]
      (let [coll (nth prov1-colls n)
            granule-count (nth prov1-granule-counts n)]
        (dotimes [m granule-count]
          (d/ingest "PROV1" (dg/granule coll)))))
    (dotimes [n prov2-collection-count]
      (let [coll (nth prov2-colls n)
            granule-count (nth prov2-granule-counts n)]
        (dotimes [m granule-count]
          (d/ingest "PROV2" (dg/granule coll)))))

    (index/refresh-elastic-index)

    {"PROV1" prov1-holdings
     "PROV2" prov2-holdings}))

(use-fixtures :each (ingest/reset-fixture {"provguid1" "PROV1" "provguid2" "PROV2"}))

(deftest retrieve-provider-holdings
  (let [all-holdings (create-holdings)
        expected-all-holdings (set (flatten (vals all-holdings)))]
    (testing "retrieve provider holdings in xml"
      (let [response (search/provider-holdings-in-format :xml)]
        (is (= 200 (:status response)))
        (is (= expected-all-holdings
               (set (:results response))))))
    (testing "retrieve provider holdings for list of providers in xml"
      (let [response (search/provider-holdings-in-format :xml {:provider_id "PROV1"})]
        (is (= 200 (:status response)))
        (is (= (set (get all-holdings "PROV1"))
               (set (:results response)))))
      (let [response (search/provider-holdings-in-format :xml {:provider_id ["PROV1" "PROV2"]})]
        (is (= 200 (:status response)))
        (is (= expected-all-holdings
               (set (:results response))))))
    (testing "as extension"
      (is (= (select-keys
               (search/provider-holdings-in-format :xml {:provider_id "PROV1"})
               [:status :results])
             (select-keys
               (search/provider-holdings-in-format :xml
                                                   {:provider_id "PROV1"}
                                                   {:format-as-ext? true})
               [:status :results]))))


    (testing "retrieve provider holdings in JSON"
      (let [response (search/provider-holdings-in-format :json)]
        (is (= 200 (:status response)))
        (is (= expected-all-holdings
               (set (:results response))))))
    (testing "retrieve provider holdings for list of providers in JSON"
      (let [response (search/provider-holdings-in-format :json {:provider_id "PROV1"})]
        (is (= 200 (:status response)))
        (is (= (set (get all-holdings "PROV1"))
               (set (:results response)))))
      (let [response (search/provider-holdings-in-format :json {:provider_id ["PROV1" "PROV2"]})]
        (is (= 200 (:status response)))
        (is (= expected-all-holdings
               (set (:results response))))))
    (testing "as extension"
      (is (= (select-keys
               (search/provider-holdings-in-format :json {:provider_id "PROV1"})
               [:status :results])
             (select-keys
               (search/provider-holdings-in-format :json
                                                   {:provider_id "PROV1"}
                                                   {:format-as-ext? true})
               [:status :results]))))))

