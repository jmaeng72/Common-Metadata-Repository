(ns cmr.metadata-db.data.util
  "This is a utility namespace for functions useful by two or more
  implementations as well as other parts of the CMR. Putting them here
  avoids having to call into an implementation."
  (:require
   [cheshire.core :as json]
   [clojure.set :as set]
   [clojure.string :as string]
   [cmr.common.mime-types :as mt]
   [cmr.common.util :as util]
   [cmr.umm-spec.umm-spec-core :as umm-spec]))

(def EXPIRED_CONCEPTS_BATCH_SIZE
  "The batch size to retrieve expired concepts"
  5000)

(def INITIAL_CONCEPT_NUM
  "The number to use as the numeric value for the first concept. Chosen to be larger than the current
  largest sequence in Catalog REST in operations which is 1005488460 as of this writing."
  1200000000)

(def mime-type->db-format-map
  "A mapping of mime type strings to the strings they are stored in the database as. The existing ones
  here match what Catalog REST stores and must continue to match that. Adding new ones is allowed
  but do not modify these existing values."
  {mt/echo10   "ECHO10"
   mt/iso-smap "ISO_SMAP"
   mt/iso19115 "ISO19115"
   mt/dif      "DIF"
   mt/dif10    "DIF10"
   mt/edn      "EDN"
   mt/umm-json "UMM_JSON"
   mt/json     "JSON"})

(defn mime-type->db-format
  [x]
  (if (mt/umm-json? x)
    (str "UMM_JSON;" (mt/version-of x))
    (get mime-type->db-format-map x)))

(def db-format->mime-type-map
  "A mapping of the format strings stored in the database to the equivalent mime type in concepts"
  ;; We add "ISO-SMAP" mapping here to work with data that are bootstrapped or synchronized directly
  ;; from catalog-rest. Since catalog-rest uses ISO-SMAP as the format value in its database and
  ;; CMR bootstrap-app simply copies this format into CMR database, we could have "ISO-SMAP" as
  ;; a format in CMR database.
  (assoc (set/map-invert mime-type->db-format-map)
         "ISO-SMAP" mt/iso-smap
         ;; We also have to support whatever the original version of the the string Metadata DB originally used.
         "SMAP_ISO" mt/iso-smap))

(defn db-format->mime-type
  [db-format]
  (if (.startsWith db-format "UMM_JSON")
    (let [[_ version] (string/split db-format #";")]
      (mt/with-version mt/umm-json (or version "1.0")))
    ;; if it's anything else, including "UMM_JSON", use the map lookup
    (get db-format->mime-type-map db-format)))

(defn metadata->umm-concept
  "Parses native metadata into umm-json and returns concept with new metadata"
  [{:keys [metadata concept-type format] :as concept} db]
  (if (or (= format (mt/format->mime-type :umm-json))
          (not (= :collection concept-type))
          (not (get-in db [:context :include-umm-metadata])))
    concept
    (let [umm-json (->> metadata
                        (umm-spec/parse-metadata
                         (assoc (:context db) :ignore-kms-keywords true)
                         concept-type
                         format)
                        util/remove-nils-empty-maps-seqs
                        json/generate-string)]
      (-> concept
          (assoc :metadata umm-json)
          (assoc :format (mt/format->mime-type :umm-json))))))

(defn validate-table-name
  "Checks if table name is valid, throws error if not valid"
  [table-name]
  (when-not (re-matches #"^[a-zA-Z0-9_]*$" table-name)
    (throw (ex-info "Table name is not valid. Can only have alphanumeric and underscore chars." {:invalid-table table-name}))))
