(ns cmr.search.results-handlers.tools-umm-json-results-handler
  "Handles tool umm-json results format and related functions"
  (:require
   [cheshire.core :as json]
   [cmr.common-app.services.search :as qs]
   [cmr.elastic-utils.search.es-index :as elastic-search-index]
   [cmr.elastic-utils.search.es-results-to-query-results :as elastic-results]
   [cmr.search.results-handlers.umm-json-results-helper :as results-helper]))

(defmethod elastic-search-index/concept-type+result-format->fields [:tool :umm-json-results]
  [concept-type query]
  results-helper/meta-fields)

(defmethod results-helper/elastic-result+metadata->umm-json-item :tool
  [concept-type elastic-result metadata]
  {:meta (results-helper/elastic-result->meta :tool elastic-result)
   :umm (json/decode metadata)})

(defmethod elastic-results/elastic-results->query-results [:tool :umm-json-results]
  [context query elastic-results]
  (results-helper/query-elastic-results->query-results context :tool query elastic-results))

(defmethod qs/search-results->response [:tool :umm-json-results]
  [context query results]
  (json/generate-string (select-keys results [:hits :took :items])))
