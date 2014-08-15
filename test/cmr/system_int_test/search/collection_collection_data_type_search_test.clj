(ns cmr.system-int-test.search.collection-collection-data-type-search-test
  "Integration test for CMR collection search by collection data type"
  (:require [clojure.test :refer :all]
            [cmr.system-int-test.utils.ingest-util :as ingest]
            [cmr.system-int-test.utils.search-util :as search]
            [cmr.system-int-test.utils.index-util :as index]
            [cmr.system-int-test.data2.collection :as dc]
            [cmr.system-int-test.data2.core :as d]))

(use-fixtures :each (ingest/reset-fixture {"provguid1" "PROV1"}))

(deftest search-by-collection-data-type
  (let [coll1 (d/ingest "PROV1" (dc/collection {:collection-data-type "NEAR_REAL_TIME"}))
        coll2 (d/ingest "PROV1" (dc/collection {:collection-data-type "SCIENCE_QUALITY"}))
        coll3 (d/ingest "PROV1" (dc/collection {:collection-data-type "OTHER"}))
        coll4 (d/ingest "PROV1" (dc/collection {}))]

    (index/refresh-elastic-index)

    (testing "search by collection data type."
      (are [value items] (d/refs-match? items (search/find-refs :collection {:collection-data-type value}))
           "NEAR_REAL_TIME" [coll1]
           "SCIENCE_QUALITY" [coll2 coll4]
           "OTHER" [coll3]
           "near_real_time" [coll1]
           "science_quality" [coll2 coll4]
           "Other" [coll3]
           ["NEAR_REAL_TIME" "OTHER"] [coll1 coll3]
           "BLAH" []))

    (testing "search by collection data type NEAR_REAL_TIME aliases."
      (are [value items] (d/refs-match? items (search/find-refs :collection {:collection-data-type value}))
           "near_real_time" [coll1]
           "nrt" [coll1]
           "NRT" [coll1]
           "near real time" [coll1]
           "near-real time" [coll1]
           "near-real-time" [coll1]
           "near real-time" [coll1]))

    (testing "search by collection data type case default is ignore case true."
      (is (d/refs-match? [coll3]
                         (search/find-refs :collection
                                           {:collection-data-type "other"}))))

    (testing "search by collection data type ignore case false."
      (is (d/refs-match? []
                         (search/find-refs :collection
                                           {:collection-data-type "science_quality"
                                            "options[collection-data-type][ignore-case]" "false"}))))

    (testing "search by collection data type ignore case true"
      (is (d/refs-match? [coll2 coll4]
                         (search/find-refs :collection
                                           {:collection-data-type "science_quality"
                                            "options[collection-data-type][ignore-case]" "true"}))))

    (testing "search by collection data type, options :and false."
      (is (d/refs-match? [coll1 coll3]
                         (search/find-refs :collection {"collection-data-type[]" ["NEAR_REAL_TIME" "OTHER"]
                                                        "options[collection-data-type][and]" "false"}))))

    (testing "search by collection data type, options :and true."
      (is (d/refs-match? []
                         (search/find-refs :collection
                                           {"collection-data-type[]" ["NEAR_REAL_TIME" "OTHER"]
                                            "options[collection-data-type][and]" "true"}))))))
