(ns cmr.spatial.circle
  (:require
   [cmr.spatial.math :as math :refer :all]
   [primitive-math]
   [cmr.spatial.point :as p]
   [cmr.spatial.derived :as d]
   [cmr.spatial.geodetic-ring :as gr]
   [cmr.spatial.validation :as sv]
   [cmr.spatial.polygon :as poly]
   [cmr.spatial.mbr :as spatial-mbr]
   [cmr.common.dev.record-pretty-printer :as record-pretty-printer])
  (:import
   cmr.spatial.geodetic_ring.GeodeticRing
   cmr.spatial.mbr.Mbr
   cmr.spatial.polygon.Polygon
   cmr.spatial.point.Point))

(primitive-math/use-primitive-operators)

;; Circle
(defrecord Circle
  [
   ^Point center
   ^double radius

   ;;derived
   ^Mbr mbr
   ])

(record-pretty-printer/enable-record-pretty-printing Circle)

(defn circle
  "Creates a new minimum bounding rectangle"
  ([^Point center ^double radius]
   (->Circle center radius nil))
  ([^double lon ^double lat ^double radius]
   (->Circle (p/point lon lat) radius nil)))

(def MIN_RADIUS
  "Minimum radius in meters"
  10)

(def MAX_RADIUS
  "Maximum radius in meters"
  1000000)

(defn validate-radius
  "Validate the radius. Returns a list of errors when radius is invalid; otherwise returns nil"
  [^double radius]
  (when-not (<= (double MIN_RADIUS) radius (double MAX_RADIUS))
    [(format "Circle radius must be between %s and %s, but was %s." MIN_RADIUS MAX_RADIUS radius)]))

(defn covers-point?
  "Returns true if the circle contains the given point"
  [^Circle cir ^Point point]
  (let [{:keys [^Point center ^double radius]} cir]
    (>= radius (p/distance center point))))

(defn circle->polygon
  "Returns the polygon approximation of the circle with the given number of points.
   This result will be less accurate for large circle or smaller n."
  [^Circle cir ^long n]
  (let [{:keys [^Point center ^double radius]} cir
        lon1 (.lon_rad center)
        lat1 (.lat_rad center)
        r-lat (/ radius EARTH_RADIUS_METERS)
        r-lon (/ r-lat (cos lat1))
        points (persistent!
                (reduce (fn [pts ^long index]
                          (let [theta (* 2.0 PI (/ (double index) (double n)))
                                plon (degrees (+ lon1 (* r-lon (cos theta))))
                                plat (degrees (+ lat1 (* r-lat (sin theta))))]
                            (conj! pts (p/point plon plat))))
                        (transient [])
                        (range 0 (inc n))))]
    (poly/polygon :geodetic [(gr/ring points)])))

(defn mbr
  "Returns the MBR of the circle. The algorithm used is documented at:
  http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates#RefBronstein"
  [^Circle cir]
  (let [{:keys [^Point center ^double radius]} cir
        lon1 (.lon_rad center)
        lat1 (.lat_rad center)
        r-rad (/ radius EARTH_RADIUS_METERS)
        delta (asin (/ (sin r-rad) (cos lat1)))
        lon-min (degrees (- lon1 delta))
        lon-max (degrees (+ lon1 delta))
        lat-min (degrees (- lat1 r-rad))
        lat-max (degrees (+ lat1 r-rad))]
    (spatial-mbr/mbr lon-min lat-max lon-max lat-min)))

(defn covers-br?
  "Returns true if the circle covers the given bounding rectangle"
  [^Circle cir ^Mbr br]
  (let [{:keys [corner-points]} br]
    (every? (partial covers-point? cir) corner-points)))

(extend-protocol d/DerivedCalculator
  cmr.spatial.circle.Circle
  (calculate-derived
    ^Circle [^Circle cir]
    (if (.mbr cir)
      cir
      (assoc cir :mbr (mbr cir)))))

(extend-protocol sv/SpatialValidation
  cmr.spatial.circle.Circle
  (validate
   [record]
   (concat (sv/validate (:center record))
           (validate-radius (:radius record)))))