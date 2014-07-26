(ns cmr.search.results-handlers.atom-json-results-handler
  "Handles the JSON results format and related functions"
  (:require [cmr.search.data.elastic-results-to-query-results :as elastic-results]
            [cmr.search.data.elastic-search-index :as elastic-search-index]
            [cmr.search.services.query-service :as qs]
            [cheshire.core :as json]
            [clj-time.core :as time]
            [cmr.search.services.url-helper :as url]
            [cmr.search.results-handlers.atom-results-handler :as atom]
            [cmr.search.results-handlers.atom-spatial-results-handler :as atom-spatial]))

(defmethod elastic-search-index/concept-type+result-format->fields [:collection :json]
  [concept-type result-format]
  (elastic-search-index/concept-type+result-format->fields :collection :atom))

(defmethod elastic-search-index/concept-type+result-format->fields [:granule :json]
  [concept-type result-format]
  (elastic-search-index/concept-type+result-format->fields :granule :atom))

(defmethod elastic-results/elastic-result->query-result-item :json
  [context query elastic-result]
  (elastic-results/elastic-result->query-result-item context (assoc query :result-format :atom) elastic-result))

(defn- collection-atom-reference->json
  "Converts a search result collection atom reference into json"
  [reference]
  (let [{:keys [id title short-name version-id summary updated dataset-id collection-data-type
                processing-level-id original-format data-center archive-center start-date end-date
                atom-links associated-difs online-access-flag browse-flag coordinate-system shapes]} reference
        shape-result (atom-spatial/shapes->json shapes)
        result (merge {:id id
                       :title title
                       :summary summary
                       :updated updated
                       :dataset_id dataset-id
                       :short_name short-name
                       :version_id version-id
                       :original_format original-format
                       :collection_data_type collection-data-type
                       :data_center data-center
                       :archive_center archive-center
                       :processing_level_id processing-level-id
                       :time_start start-date
                       :time_end end-date
                       :dif_ids associated-difs
                       :online_access_flag online-access-flag
                       :browse_flag browse-flag
                       :links (map atom/atom-link->attribute-map atom-links)
                       :coordinate_system coordinate-system}
                      shape-result)]
    ;; remove entries with nil value
    (apply dissoc
           result
           (for [[k v] result :when (nil? v)] k))))

(defn- granule-atom-reference->json
  "Converts a search result granule atom reference into json"
  [reference]
  (let [{:keys [id title updated dataset-id producer-gran-id size original-format
                data-center start-date end-date atom-links online-access-flag browse-flag
                day-night cloud-cover coordinate-system shapes]} reference
        shape-result (atom-spatial/shapes->json shapes)
        result (merge {:id id
                       :title title
                       :updated updated
                       :dataset_id dataset-id
                       :producer_granule_id producer-gran-id
                       :granule_size size
                       :original_format original-format
                       :data_center data-center
                       :time_start start-date
                       :time_end end-date
                       :links (map atom/atom-link->attribute-map atom-links)
                       :online_access_flag online-access-flag
                       :browse_flag browse-flag
                       :day_night_flag day-night
                       :cloud_cover cloud-cover
                       :coordinate_system coordinate-system}
                      shape-result)]
    ;; remove entries with nil value
    (apply dissoc
           result
           (for [[k v] result :when (nil? v)] k))))

(defmethod qs/search-results->response-json :json
  [context query results]
  (let [{:keys [items]} results
        {:keys [concept-type result-format]} query
        atom-reference-to-json-fn (if (= :granule concept-type) granule-atom-reference->json
                                    collection-atom-reference->json)
        items (if (= :granule (:concept-type query))
                (atom/append-collection-links context items)
                items)
        response-results {:feed
                          {:updated (str (time/now))
                           :id (url/atom-request-url context concept-type result-format)
                           :title (atom/concept-type->atom-title (:concept-type query))
                           :entry (map atom-reference-to-json-fn items)}}]
    response-results))

(defmethod qs/search-results->response :json
  [context query results]
  (let [response-results (qs/search-results->response-json context query results)]
    (json/generate-string response-results {:pretty (:pretty? query)})))
