(ns cmr.metadata-db.test.data.oracle.granule-table
  (:require
  [clojure.test :refer :all]
  [cmr.metadata-db.data.oracle.granule-table :as gt]
  [cmr.common.util :as util :refer [are3]]))

(deftest granule-constraint-sql-false-test
  (testing "valid table name"
    (are3 [table-name query]
          (let [non-small-provider {:provider-id "PROV1", :short-name "test provider", :cmr-only false, :small false}]
            (is (= query (gt/granule-constraint-sql non-small-provider table-name))))

            "valid table name"
            "table_name"
            "CONSTRAINT table_name_pk PRIMARY KEY (id), CONSTRAINT table_name_con_rev\n               UNIQUE (native_id, revision_id)\n               USING INDEX (create unique index table_name_ucr_i\n               ON table_name (native_id, revision_id)), CONSTRAINT table_name_cid_rev\n               UNIQUE (concept_id, revision_id)\n               USING INDEX (create unique index table_name_cri\n               ON table_name (concept_id, revision_id))"

            "valid table name with numbers"
            "1234tablename__"
            "CONSTRAINT 1234tablename___pk PRIMARY KEY (id), CONSTRAINT 1234tablename___con_rev\n               UNIQUE (native_id, revision_id)\n               USING INDEX (create unique index 1234tablename___ucr_i\n               ON 1234tablename__ (native_id, revision_id)), CONSTRAINT 1234tablename___cid_rev\n               UNIQUE (concept_id, revision_id)\n               USING INDEX (create unique index 1234tablename___cri\n               ON 1234tablename__ (concept_id, revision_id))"))
  (testing "invalid table name"
    (are3 [table-name]
          (let [non-small-provider {:provider-id "PROV1", :short-name "test provider", :cmr-only false, :small false}]
            (is (thrown? Exception (gt/granule-constraint-sql non-small-provider table-name))))

          "invalid table name"
          "=table_name--;"
          true

          "invalid table name 2"
          "table_; DELETE"
          true)))

(deftest granule-constraint-sql-true-test
  (testing "valid table name"
    (are3 [table-name query]
          (let [small-provider {:provider-id "PROV1", :short-name "test provider", :cmr-only false, :small true}]
            (is (= query (gt/granule-constraint-sql small-provider table-name))))

          "valid table name"
          "table_name"
          "CONSTRAINT table_name_pk PRIMARY KEY (id), CONSTRAINT table_name_con_rev\n               UNIQUE (provider_id, native_id, revision_id)\n               USING INDEX (create unique index table_name_ucr_i\n               ON table_name (provider_id, native_id, revision_id)), CONSTRAINT table_name_cid_rev\n               UNIQUE (concept_id, revision_id)\n               USING INDEX (create unique index table_name_cri\n               ON table_name (concept_id, revision_id))"

          "valid table name with numbers"
          "1234tablename__"
          "CONSTRAINT 1234tablename___pk PRIMARY KEY (id), CONSTRAINT 1234tablename___con_rev\n               UNIQUE (provider_id, native_id, revision_id)\n               USING INDEX (create unique index 1234tablename___ucr_i\n               ON 1234tablename__ (provider_id, native_id, revision_id)), CONSTRAINT 1234tablename___cid_rev\n               UNIQUE (concept_id, revision_id)\n               USING INDEX (create unique index 1234tablename___cri\n               ON 1234tablename__ (concept_id, revision_id))"))
  (testing "invalid table name"
    (are3 [table-name]
          (let [small-provider {:provider-id "PROV1", :short-name "test provider", :cmr-only false, :small true}]
            (is (thrown? Exception (gt/granule-constraint-sql small-provider table-name))))

          "invalid table name"
          "=table_name--;"
          true

          "invalid table name 2"
          "table_; DELETE"
          true)))

(deftest create-common-gran-indexes
  (testing "invalid table name"
    (are3 [table-name]
          (let [fun #'gt/create-common-gran-indexes]
            (is (thrown? Exception (fun nil table-name))))

          "invalid table name"
          "=table_name--;"
          true

          "invalid table name 2"
          "table_; DELETE"
          true)))

(deftest create-granule-indexes-true-test
  (testing "invalid table name"
    (are3 [table-name]
          (let [small-provider {:provider-id "PROV1", :short-name "test provider", :cmr-only false, :small true}]
            (is (thrown? Exception (gt/create-granule-indexes nil small-provider table-name))))

          "invalid table name"
          "=table_name--;"
          true

          "invalid table name 2"
          "table_; DELETE"
          true)))

(deftest create-granule-indexes-false-test
  (testing "invalid table name"
    (are3 [table-name]
          (let [non-small-provider {:provider-id "PROV1", :short-name "test provider", :cmr-only false, :small false}]
            (is (thrown? Exception (gt/create-granule-indexes nil non-small-provider table-name))))

          "invalid table name"
          "=table_name--;"
          true

          "invalid table name 2"
          "table_; DELETE"
          true)))
