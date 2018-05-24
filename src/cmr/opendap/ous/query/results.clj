(ns cmr.opendap.ous.query.results)

(defrecord CollectionResults
  [;; The number of results returned
   hits
   ;; Number of milleseconds elapsed from start to end of call
   took
   ;; The actual items in the result set
   items])

(defn create
  [results & {:keys [elapsed]}]
  (map->CollectionResults
    {;; Our 'hits' is simplistic for now; will change when we support
     ;; paging, etc.
     :hits (count results)
     :took elapsed
     :items results}))

(defn elided
  [results]
  (if (seq results)
    (assoc results :items [(first (:items results) )"..."])
    nil))

(defn remaining-items
  [results]
  (if (seq results)
    (rest (:items results))
    nil))
