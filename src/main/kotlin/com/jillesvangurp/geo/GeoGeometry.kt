/*
 * Copyright (c) 2012-2019, Jilles van Gurp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jillesvangurp.geo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The methods in this class provides methods that may be used to manipulate geometric shapes. The methods follow the
 * GeoJson http://geojson.org/ convention of expressing shapes as multi dimensional arrays of points.
 *
 * Following this convention means there is no need for an object oriented framework to represent the different shapes.
 * Consequently, all of the methods in this framework are static methods. This makes usage of these methods very
 * straightforward and also makes it easy to integrate with the many frameworks out there that provide their own object
 * oriented abstractions.
 *
 * So, a point is an array with the coordinate pair. A line (or line string) is a 2d array of points. A polygon is a 3d
 * array that consists of an outer polygon and zero or more inner polygons (holes). Each 2d array should be a closed
 * linear ring where the last point is the same as the first point.
 *
 * Finally, 4d arrays can be used to express multipolygons of one or more polygons (each with their own holes).
 *
 * It should be noted that GeoJson represents points as arrays of [longitude, latitude] rather than the conventional way
 * of latitude followed by longitude.
 *
 * It should also be noted that this class contains several methods that treat 2d arrays as polygons.
 */
@Suppress("unused")
class GeoGeometry {

    companion object {
        /**
         * Earth's mean radius, in meters.
         *
         * see http://en.wikipedia.org/wiki/Earth%27s_radius#Mean_radii
         */
        const val EARTH_RADIUS_METERS = 6371000.0
        const val EARTH_CIRCUMFERENCE_METERS = EARTH_RADIUS_METERS * PI * 2.0
        const val DEGREE_LATITUDE_METERS = EARTH_RADIUS_METERS * PI / 180.0
        const val DEGREES_TO_RADIANS = 2 * PI / 360

        /**
         * @param point point
         * @return bounding box that contains the point as a double array of
         * [lat,lat,lon,lon}
         */
        @JvmStatic
        fun boundingBox(point: Point): BoundingBox {
            return doubleArrayOf(point[1], point[1], point[0], point[0])
        }

        /**
         * @param points line
         * @return bounding box that contains the lineString as a double array of
         * [minLat,maxLat,minLon,maxLon}
         */
        @JvmStatic
        fun boundingBox(points: MultiPoint): BoundingBox {
            var minLat = Integer.MAX_VALUE.toDouble()
            var minLon = Integer.MAX_VALUE.toDouble()
            var maxLat = Integer.MIN_VALUE.toDouble()
            var maxLon = Integer.MIN_VALUE.toDouble()

            for (doubles in points) {
                minLat = min(minLat, doubles[1])
                minLon = min(minLon, doubles[0])
                maxLat = max(maxLat, doubles[1])
                maxLon = max(maxLon, doubles[0])
            }

            return doubleArrayOf(minLat, maxLat, minLon, maxLon)
        }

        /**
         * @param polygon 3d polygon array
         * @return bounding box that contains the polygon as a double array of
         * [minLat,maxLat,minLon,maxLon}
         */
        @JvmStatic
        fun boundingBox(polygon: Polygon): BoundingBox {
            var minLat = Integer.MAX_VALUE.toDouble()
            var minLon = Integer.MAX_VALUE.toDouble()
            var maxLat = Integer.MIN_VALUE.toDouble()
            var maxLon = Integer.MIN_VALUE.toDouble()
            for (linearRing in polygon) {
                for (point in linearRing) {
                    minLat = min(minLat, point[1])
                    minLon = min(minLon, point[0])
                    maxLat = max(maxLat, point[1])
                    maxLon = max(maxLon, point[0])
                }
            }
            return doubleArrayOf(minLat, maxLat, minLon, maxLon)
        }

        /**
         * @param multiPolygon 4d multipolygon array
         * @return bounding box that contains the multiPolygon as a double array of
         * [minLat,maxLat,minLon,maxLon}
         */
        @JvmStatic
        fun boundingBox(multiPolygon: MultiPolygon): BoundingBox {
            var minLat = Integer.MAX_VALUE.toDouble()
            var minLon = Integer.MAX_VALUE.toDouble()
            var maxLat = Integer.MIN_VALUE.toDouble()
            var maxLon = Integer.MIN_VALUE.toDouble()
            for (polygon in multiPolygon) {
                for (linearRing in polygon) {
                    for (point in linearRing) {
                        minLat = min(minLat, point[1])
                        minLon = min(minLon, point[0])
                        maxLat = max(maxLat, point[1])
                        maxLon = max(maxLon, point[0])
                    }
                }
            }
            return doubleArrayOf(minLat, maxLat, minLon, maxLon)
        }

        /**
         * Points in a cloud are supposed to be close together. Sometimes bad data causes a handful of points out of
         * thousands to be way off. This method filters those out by sorting the coordinates and then discarding the
         * specified percentage.
         *
         * @param points 2d array of points
         * @param percentage percentage of points to discard
         * @return sorted array of points with the specified percentage of elements at the beginning and end of the array removed.
         */
        @JvmStatic
        fun filterNoiseFromPointCloud(points: MultiPoint, percentage: Float): MultiPoint {

            points.sortWith(Comparator { p1, p2 ->
                when {
                    p1 == null || p2 == null -> throw IllegalArgumentException("Array contains null points")
                    p1[0] == p2[0] -> p1[1].compareTo(p2[1])
                    p1[0] > p2[0] -> 1
                    p1[0] == p2[0] -> 0
                    else -> -1
                }
            })

            val discard = (points.size * percentage / 2).toInt()

            return points.copyOfRange(discard, points.size - discard)
        }

        /**
         * @param bbox
         * double array of [minLat,maxLat,minLon,maxLon}
         * @param latitude latitude
         * @param longitude longitude
         * @return true if the latitude and longitude are contained in the bbox
         */
        @JvmStatic
        fun bboxContains(bbox: BoundingBox, latitude: Double, longitude: Double): Boolean {
            validate(latitude, longitude, false)
            return bbox[0] <= latitude && latitude <= bbox[1] && bbox[2] <= longitude && longitude <= bbox[3]
        }

        /**
         * Determine whether a point is contained in a polygon. Note, technically
         * the points that make up the polygon are not contained by it.
         *
         * @param point point
         * @param polygonPoints 3d array representing a geojson polygon. Note. the polygon holes are ignored currently.
         * @return true if the polygon contains the coordinate
         */
        @JvmStatic
        fun polygonContains(point: Point, polygonPoints: Polygon): Boolean {
            validate(point)
            return polygonContains(point[1], point[0], *polygonPoints[0])
        }

        /**
         * Determine whether a point is contained in a polygon. Note, technically
         * the points that make up the polygon are not contained by it.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param polygonPoints 3d array representing a geojson polygon. Note. the polygon holes are ignored currently.
         * @return true if the polygon contains the coordinate
         */
        @JvmStatic
        fun polygonContains(latitude: Double, longitude: Double, polygonPoints: Polygon): Boolean {
            validate(latitude, longitude, false)
            return polygonContains(latitude, longitude, *polygonPoints[0])
        }

        /**
         * Determine whether a point is contained in a polygon. Note, technically
         * the points that make up the polygon are not contained by it.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param polygonPoints
         * polygonPoints points that make up the polygon as arrays of
         * [longitude,latitude]
         * @return true if the polygon contains the coordinate
         */
        @JvmStatic
        fun polygonContains(latitude: Double, longitude: Double, vararg polygonPoints: Point): Boolean {
            validate(latitude, longitude, false)

            if (polygonPoints.size <= 2) {
                throw IllegalArgumentException("a polygon must have at least three points")
            }
            @Suppress("UNCHECKED_CAST") val bbox = boundingBox(polygonPoints as Array<DoubleArray>)
            if (!bboxContains(bbox, latitude, longitude)) {
                // outside the containing bbox
                return false
            }

            var hits = 0

            var lastLatitude = polygonPoints[polygonPoints.size - 1][1]
            var lastLongitude = polygonPoints[polygonPoints.size - 1][0]
            var currentLatitude: Double
            var currentLongitude: Double

            // Walk the edges of the polygon
            var i = 0
            while (i < polygonPoints.size) {
                currentLatitude = polygonPoints[i][1]
                currentLongitude = polygonPoints[i][0]

                if (currentLongitude == lastLongitude) {
                    lastLatitude = currentLatitude
                    lastLongitude = currentLongitude
                    i++
                    continue
                }

                val leftLatitude: Double
                if (currentLatitude < lastLatitude) {
                    if (latitude >= lastLatitude) {
                        lastLatitude = currentLatitude
                        lastLongitude = currentLongitude
                        i++
                        continue
                    }
                    leftLatitude = currentLatitude
                } else {
                    if (latitude >= currentLatitude) {
                        lastLatitude = currentLatitude
                        lastLongitude = currentLongitude
                        i++
                        continue
                    }
                    leftLatitude = lastLatitude
                }

                val test1: Double
                val test2: Double
                if (currentLongitude < lastLongitude) {
                    if (longitude < currentLongitude || longitude >= lastLongitude) {
                        lastLatitude = currentLatitude
                        lastLongitude = currentLongitude
                        i++
                        continue
                    }
                    if (latitude < leftLatitude) {
                        hits++
                        lastLatitude = currentLatitude
                        lastLongitude = currentLongitude
                        i++
                        continue
                    }
                    test1 = latitude - currentLatitude
                    test2 = longitude - currentLongitude
                } else {
                    if (longitude < lastLongitude || longitude >= currentLongitude) {
                        lastLatitude = currentLatitude
                        lastLongitude = currentLongitude
                        i++
                        continue
                    }
                    if (latitude < leftLatitude) {
                        hits++
                        lastLatitude = currentLatitude
                        lastLongitude = currentLongitude
                        i++
                        continue
                    }
                    test1 = latitude - lastLatitude
                    test2 = longitude - lastLongitude
                }

                if (test1 < test2 / (lastLongitude - currentLongitude) * (lastLatitude - currentLatitude)) {
                    hits++
                }
                lastLatitude = currentLatitude
                lastLongitude = currentLongitude
                i++
            }

            return hits and 1 != 0
        }

        /**
         * Simple rounding method that allows you to get rid of some decimals in a
         * double.
         *
         * @param d a double
         * @param decimals the number of desired decimals after the .
         * @return d rounded to the specified precision
         */
        @JvmStatic
        fun roundToDecimals(d: Double, decimals: Int): Double {
            if (decimals > 17) {
                throw IllegalArgumentException("this probably doesn't do what you want; makes sense only for <= 17 decimals")
            }
            val factor = 10.0.pow(decimals.toDouble())
            return (d * factor).roundToLong() / factor
        }

        @JvmStatic
        fun linesCross(l1p1: Point, l1p2: Point, l2p1: Point, l2p2: Point): Boolean {
            return linesCross(l1p1.longitude, l1p1.latitude, l1p2.longitude, l1p2.latitude, l2p1.longitude, l2p1.latitude, l2p2.longitude, l2p2.latitude)
        }

        /**
         * Check if the line segments defined by  (x1,y1) (x2,y2) and (u1,v1) (u2,v2) cross each other or not.
         * @param x1 double
         * @param y1 double
         * @param x2 double
         * @param y2 double
         * @param u1 double
         * @param v1 double
         * @param u2 double
         * @param v2 double
         * @return true if they cross each other
         */
        @JvmStatic
        fun linesCross(
            x1: Double,
            y1: Double,
            x2: Double,
            y2: Double,
            u1: Double,
            v1: Double,
            u2: Double,
            v2: Double
        ): Boolean {
            // formula for line: y= a+bx

            // vertical lines result in a divide by 0;
            val line1Vertical = x1 == x2
            val line2Vertical = u1 == u2
            return when {
                line1Vertical && line2Vertical -> // x=a
                    if (x1 == u1) {
                        // lines are the same, check whether they overlap
                        y1 <= v1 && v1 < y2 || y1 <= v2 && v2 < y2
                    } else {
                        // parallel -> they don't intersect!
                        false
                    }
                line1Vertical -> {
                    val gradient2 = (v2 - v1) / (u2 - u1)

                    val a2 = v1 - gradient2 * u1

                    val yi = a2 + gradient2 * x1

                    isBetween(y1, y2, yi) && isBetween(v1, v2, yi)
                }
                line2Vertical -> {
                    val gradient1 = (y2 - y1) / (x2 - x1)
                    val a1 = y1 - gradient1 * x1

                    val yi = a1 + gradient1 * u1

                    isBetween(y1, y2, yi) && isBetween(v1, v2, yi)
                }
                else -> {

                    val gradient1 = (y2 - y1) / (x2 - x1)
                    // divide by zero if second line vertical
                    val gradient2 = (v2 - v1) / (u2 - u1)

                    val a1 = y1 - gradient1 * x1
                    val a2 = v1 - gradient2 * u1

                    if (gradient1 - gradient2 == 0.0) {
                        // same gradient
                        if (abs(a1 - a2) < .0000001) {
                            // lines are definitely the same within a margin of error, check if their x overlaps
                            isBetween(x1, x2, u1) || isBetween(x1, x2, u2)
                        } else {
                            // parallel -> they don't intersect!
                            false
                        }
                    } else {
                        // calculate intersection point xi,yi
                        val xi = -(a1 - a2) / (gradient1 - gradient2)
                        val yi = a1 + gradient1 * xi

                        (x1 - xi) * (xi - x2) >= 0 &&
                            (u1 - xi) * (xi - u2) >= 0 &&
                            (y1 - yi) * (yi - y2) >= 0 &&
                            (v1 - yi) * (yi - v2) >= 0
                    }
                }
            }
        }

        private fun isBetween(x1: Double, x2: Double, value: Double): Boolean {
            return if (x1 > x2) {
                value in x2..x1
            } else {
                value in x1..x2
            }
        }

        private fun lengthOfLongitudeDegreeAtLatitude(latitude: Double): Double {
            val latitudeInRadians = toRadians(latitude)
            return cos(latitudeInRadians) * EARTH_CIRCUMFERENCE_METERS / 360.0
        }

        /**
         * Translate a point along the longitude for the specified amount of meters.
         * Note, this method assumes the earth is a sphere and the result is not
         * going to be very precise for larger distances.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param meters distance in meters that the point should be translated
         * @return the translated coordinate.
         */
        @JvmStatic
        fun translateLongitude(latitude: Double, longitude: Double, meters: Double): Point {
            validate(latitude, longitude, false)
            return doubleArrayOf(
                roundToDecimals(longitude + meters / lengthOfLongitudeDegreeAtLatitude(latitude), 6),
                latitude
            )
        }

        /**
         * Translate a point along the latitude for the specified amount of meters.
         * Note, this method assumes the earth is a sphere and the result is not
         * going to be very precise for larger distances.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param meters distance in meters that the point should be translated
         * @return the translated coordinate.
         */
        @JvmStatic
        fun translateLatitude(latitude: Double, longitude: Double, meters: Double): Point {
            return doubleArrayOf(longitude, roundToDecimals(latitude + meters / DEGREE_LATITUDE_METERS, 6))
        }

        /**
         * Translate a point by the specified meters along the longitude and
         * latitude. Note, this method assumes the earth is a sphere and the result
         * is not going to be very precise for larger distances.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param latitudalMeters distance in meters that the point should be translated along the latitude
         * @param longitudalMeters distance in meters that the point should be translated along the longitude
         * @return the translated coordinate.
         */
        @JvmStatic
        fun translate(
            latitude: Double,
            longitude: Double,
            latitudalMeters: Double,
            longitudalMeters: Double
        ): DoubleArray {
            validate(latitude, longitude, false)
            val longitudal = translateLongitude(latitude, longitude, longitudalMeters)
            return translateLatitude(longitudal[1], longitudal[0], latitudalMeters)
        }

        /**
         * Calculate a bounding box of the specified longitudal and latitudal meters with the latitude/longitude as the center.
         * @param latitude latitude
         * @param longitude longitude
         * @param latitudalMeters distance in meters that the point should be translated along the latitude
         * @param longitudalMeters distance in meters that the point should be translated along the longitude
         * @return [maxlat,minlat,maxlon,minlon]
         */
        @JvmStatic
        fun bbox(latitude: Double, longitude: Double, latitudalMeters: Double, longitudalMeters: Double): DoubleArray {
            validate(latitude, longitude, false)

            val topRight = translate(latitude, longitude, latitudalMeters / 2, longitudalMeters / 2)
            val bottomRight = translate(latitude, longitude, -latitudalMeters / 2, longitudalMeters / 2)
            val bottomLeft = translate(latitude, longitude, -latitudalMeters / 2, -longitudalMeters / 2)

            // FIXME make it a proper bbox and use [southLat,northLat, westLon, eastLon]
            return doubleArrayOf(topRight[1], bottomRight[1], topRight[0], bottomLeft[0])
        }

        /**
         * Kotlin math seems to not have this unlike Java  But it is easily replicated like this
         */
        @JvmStatic
        fun toRadians(degrees: Double): Double {
            return degrees * DEGREES_TO_RADIANS
        }

        /**
         * Compute the Haversine distance between the two coordinates. Haversine is
         * one of several distance calculation algorithms that exist. It is not very
         * precise in the sense that it assumes the earth is a perfect sphere, which
         * it is not. This means precision drops over larger distances. According to
         * http://en.wikipedia.org/wiki/Haversine_formula there is a 0.5% error
         * margin given the 1% difference in curvature between the equator and the
         * poles.
         *
         * @param lat1
         * the latitude in decimal degrees
         * @param long1
         * the longitude in decimal degrees
         * @param lat2
         * the latitude in decimal degrees
         * @param long2
         * the longitude in decimal degrees
         * @return the distance in meters
         */
        @JvmStatic
        fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
            validate(lat1, long1, false)
            validate(lat2, long2, false)

            val deltaLat = toRadians(lat2 - lat1)
            val deltaLon = toRadians(long2 - long1)

            val a =
                sin(deltaLat / 2) * sin(deltaLat / 2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(
                    deltaLon / 2
                ) * sin(
                    deltaLon / 2
                )

            val c = 2 * asin(sqrt(a))

            return EARTH_RADIUS_METERS * c
        }

        /**
         * Variation of the haversine distance method that takes an array
         * representation of a coordinate.
         *
         * @param p1
         * [latitude, longitude]
         * @param p2
         * [latitude, longitude]
         * @return the distance in meters
         */
        @JvmStatic
        fun distance(p1: Point, p2: Point): Double {
            return distance(p1[1], p1[0], p2[1], p2[0])
        }

        /**
         * Calculate distance of a point (pLat,pLon) to a line defined by two other points (lat1,lon1) and (lat2,lon2)
         * @param x1 double
         * @param y1 double
         * @param x2 double
         * @param y2 double
         * @param x double
         * @param y double
         * @return the distance of the point to the line
         */
        @JvmStatic
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double, x: Double, y: Double): Double {
            validate(x1, y1, false)
            validate(x2, y2, false)
            validate(x, y, false)
            val xx: Double
            val yy: Double
            when {
                y1 == y2 -> {
                    // horizontal line
                    xx = x
                    yy = y1
                }
                x1 == x2 -> {
                    // vertical line
                    xx = x1
                    yy = y
                }
                else -> {
                    // y=s*x  +c
                    val s = (y2 - y1) / (x2 - x1)
                    val c = y1 - s * x1

                    // y=ps*x + pc
                    val ps = -1 / s
                    val pc = y - ps * x

                    // solve    ps*x +pc = s*x + c
                    //          (ps-s) *x = c -pc
                    //          x= (c-pc)/(ps-s)
                    xx = (c - pc) / (ps - s)
                    yy = s * xx + c
                }
            }
            return if (onSegment(xx, yy, x1, y1, x2, y2)) {
                distance(x, y, xx, yy)
            } else {
                min(distance(x, y, x1, y1), distance(x, y, x2, y2))
            }
        }

        private fun onSegment(x: Double, y: Double, x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
            val minx = min(x1, x2)
            val maxx = max(x1, x2)

            val miny = min(y1, y2)
            val maxy = max(y1, y2)

            return x in minx..maxx && y >= miny && y <= maxy
        }

        /**
         * Calculate distance of a point p to a line defined by two other points l1 and l2.
         * @param l1 point 1 on the line
         * @param l2 point 2 on the line
         * @param p point
         * @return the distance of the point to the line
         */
        @JvmStatic
        fun distance(l1: Point, l2: Point, p: Point): Double {
            return distance(l1[1], l1[0], l2[1], l2[0], p[1], p[0])
        }

        /**
         * @param point point
         * @param lineString linestring
         * @return the distance of the point to the line
         */
        @JvmStatic
        fun distanceToLineString(point: Point, lineString: LineString): Double {
            if (lineString.size < 2) {
                throw IllegalArgumentException("not enough segments in line")
            }
            var minDistance = java.lang.Double.MAX_VALUE
            var last = lineString[0]
            for (i in 1 until lineString.size) {
                val current = lineString[i]
                val distance = distance(last, current, point)
                minDistance = min(minDistance, distance)
                last = current
            }
            return minDistance
        }

        /**
         * @param point point
         * @param polygon 2d linestring that is a polygon
         * @return distance to polygon
         */
        @JvmStatic
        fun distanceToPolygon(point: Point, polygon: LinearRing): Double {
            if (polygon.size < 3) {
                throw IllegalArgumentException("not enough segments in polygon")
            }
            return if (polygonContains(point[1], point[0], *polygon)) {
                0.0
            } else distanceToLineString(point, polygon)
        }

        /**
         * @param point point
         * @param polygon polygon
         * @return distance to polygon
         */
        @JvmStatic
        fun distanceToPolygon(point: Point, polygon: Polygon): Double {
            if (polygon.isEmpty()) {
                throw IllegalArgumentException("empty polygon")
            }
            return distanceToPolygon(point, polygon[0])
        }

        /**
         * @param point point
         * @param multiPolygon multi polygon
         * @return distance to the nearest of the polygons in the multipolygon
         */
        @JvmStatic
        fun distanceToMultiPolygon(point: DoubleArray, multiPolygon: Array<Array<Array<DoubleArray>>>): Double {
            var distance = java.lang.Double.MAX_VALUE
            for (polygon in multiPolygon) {
                distance = min(distance, distanceToPolygon(point, polygon))
            }
            return distance
        }

        /**
         * Simple/naive method for calculating the center of a polygon based on
         * averaging the latitude and longitude. Better algorithms exist but this
         * may be good enough for most purposes.
         * Note, for some polygons, this may actually be located outside the
         * polygon.
         *
         * @param polygonPoints
         * polygonPoints points that make up the polygon as arrays of
         * [longitude,latitude]
         * @return the average longitude and latitude an array.
         */
        @JvmStatic
        fun polygonCenter(vararg polygonPoints: DoubleArray): DoubleArray {
            var cumLon = 0.0
            var cumLat = 0.0
            for (coordinate in polygonPoints) {
                cumLon += coordinate[0]
                cumLat += coordinate[1]
            }
            return doubleArrayOf(cumLon / polygonPoints.size, cumLat / polygonPoints.size)
        }

        /**
         * @param bbox 2d array of [lat,lat,lon,lon]
         * @return a 2d linestring with a rectangular polygon
         */
        @JvmStatic
        fun bbox2polygon(bbox: DoubleArray): Array<DoubleArray> {
            return arrayOf(
                doubleArrayOf(bbox[2], bbox[0]),
                doubleArrayOf(bbox[2], bbox[1]),
                doubleArrayOf(bbox[3], bbox[1]),
                doubleArrayOf(bbox[3], bbox[0]),
                doubleArrayOf(bbox[2], bbox[0])
            )
        }

        /**
         * Converts a circle to a polygon.
         * This method does not behave very well very close to the poles because the math gets a little funny there.
         *
         * @param segments
         * number of segments the polygon should have. The higher this
         * number, the better of an approximation the polygon is for the
         * circle.
         * @param latitude latitude
         * @param longitude longitude
         * @param radius radius of the circle
         * @return a linestring polygon
         */
        @JvmStatic
        fun circle2polygon(segments: Int, latitude: Double, longitude: Double, radius: Double): Array<DoubleArray> {
            validate(latitude, longitude, false)

            if (segments < 5) {
                throw IllegalArgumentException("you need a minimum of 5 segments")
            }
            val points = Array(segments + 1) { DoubleArray(0) }

            val relativeLatitude = radius / EARTH_RADIUS_METERS * 180 / PI

            // things get funny near the north and south pole, so doing a modulo 90
            // to ensure that the relative amount of degrees doesn't get too crazy.
            val relativeLongitude = relativeLatitude / cos(toRadians(latitude)) % 90

            for (i in 0 until segments) {
                // radians go from 0 to 2*PI; we want to divide the circle in nice
                // segments
                var theta = 2.0 * PI * i.toDouble() / segments
                // trying to avoid theta being exact factors of pi because that results in some funny behavior around the
                // north-pole
                theta += 0.1
                if (theta >= 2 * PI) {
                    theta = theta - 2 * PI
                }

                // on the unit circle, any point of the circle has the coordinate
                // cos(t),sin(t) where t is the radian. So, all we need to do that
                // is multiply that with the relative latitude and longitude
                // note, latitude takes the role of y, not x. By convention we
                // always note latitude, longitude instead of the other way around
                var latOnCircle = latitude + relativeLatitude * sin(theta)
                var lonOnCircle = longitude + relativeLongitude * cos(theta)
                if (lonOnCircle > 180) {
                    lonOnCircle = -180 + (lonOnCircle - 180)
                } else if (lonOnCircle < -180) {
                    lonOnCircle = 180 - (lonOnCircle + 180)
                }

                if (latOnCircle > 90) {
                    latOnCircle = 90 - (latOnCircle - 90)
                } else if (latOnCircle < -90) {
                    latOnCircle = -90 - (latOnCircle + 90)
                }

                points[i] = doubleArrayOf(lonOnCircle, latOnCircle)
            }
            // should end with same point as the origin
            points[points.size - 1] = doubleArrayOf(points[0][0], points[0][1])
            return points
        }

        /**
         * @param left a 2d array representing a polygon
         * @param right a 2d array representing a polygon
         * @return true if the two polygons overlap. Important: this only works for simple convex polygons. Overlap for concave polygons is more complicated.
         */
        @JvmStatic
        fun overlap(left: LinearRing, right: LinearRing): Boolean {
            val point1 = polygonCenter(*right)
            if (polygonContains(point1[1], point1[0], *left)) {
                return true
            }
            val point = polygonCenter(*left)
            if (polygonContains(point[1], point[0], *right)) {
                return true
            }

            for (p in right) {
                if (polygonContains(p[1], p[0], *left)) {
                    return true
                }
            }
            for (p in left) {
                if (polygonContains(p[1], p[0], *right)) {
                    return true
                }
            }

            return false
        }

        /**
         * @param containingPolygon linestring polygon
         * @param containedPolygon linestring polygon
         * @return true if the containing polygon fully contains the contained polygon. Important: this only works for simple convex polygons. Contains for concave polygons is more complicated. https://en.wikipedia.org/wiki/Point_in_polygon
         */
        @JvmStatic
        fun contains(containingPolygon: Array<DoubleArray>, containedPolygon: Array<DoubleArray>): Boolean {
            for (p in containedPolygon) {
                if (!polygonContains(p[1], p[0], *containingPolygon)) {
                    return false
                }
            }
            return true
        }

        /**
         * Attempts to expand the polygon by calculating points around each of the polygon points that are translated the
         * specified amount of meters away. A new polygon is constructed from the resulting point cloud.
         *
         * Given that the contains algorithm disregards polygon points as not contained in the polygon, it is useful to
         * expand the polygon a little if you do require this.
         *
         * @param meters distance
         * @param points linestring polygon
         * @return a new polygon that fully contains the old polygon and is roughly the specified meters wider.
         */
        @JvmStatic
        fun expandPolygon(meters: Int, points: Array<DoubleArray>): Array<DoubleArray> {
            val expanded = Array(points.size * 8) { DoubleArray(0) }
            for (i in points.indices) {
                val p = points[i]
                val lonPos = translateLongitude(p[0], p[1], meters.toDouble())[0]
                val lonNeg = translateLongitude(p[0], p[1], (-1 * meters).toDouble())[0]
                val latPos = translateLatitude(p[0], p[1], meters.toDouble())[1]
                val latNeg = translateLatitude(p[0], p[1], (-1 * meters).toDouble())[1]
                expanded[i * 8] = doubleArrayOf(lonPos, latPos)
                expanded[i * 8 + 1] = doubleArrayOf(lonPos, latNeg)
                expanded[i * 8 + 2] = doubleArrayOf(lonNeg, latPos)
                expanded[i * 8 + 3] = doubleArrayOf(lonNeg, latNeg)

                expanded[i * 8 + 4] = doubleArrayOf(lonPos, p[1])
                expanded[i * 8 + 5] = doubleArrayOf(lonNeg, p[1])
                expanded[i * 8 + 6] = doubleArrayOf(p[0], latPos)
                expanded[i * 8 + 7] = doubleArrayOf(p[1], latNeg)
            }
            return polygonForPoints(expanded)
        }

        /**
         * Calculate a polygon for the specified points.
         * @param points linestring polygon
         * @return a convex polygon for the points
         */
        @JvmStatic
        fun polygonForPoints(points: MultiPoint): LinearRing {
            if (points.size < 3) {
                throw IllegalStateException("need at least 3 pois for a polygon")
            }
            val sorted = points.clone()
            sorted.sortWith(Comparator { p1, p2 ->
                when {
                    p1 == null || p2 == null -> throw IllegalArgumentException("Points array contains null")
                    p1[0] == p2[0] -> p1[1].compareTo(p2[1])
                    else -> p1[0].compareTo(p2[0])
                }
            })

            val n = sorted.size

            val lUpper = Array(n) { DoubleArray(0) }

            lUpper[0] = sorted[0]
            lUpper[1] = sorted[1]

            var lUpperSize = 2

            for (i in 2 until n) {
                lUpper[lUpperSize] = sorted[i]
                lUpperSize++

                while (lUpperSize > 2 && !rightTurn(
                        lUpper[lUpperSize - 3],
                        lUpper[lUpperSize - 2],
                        lUpper[lUpperSize - 1]
                    )
                ) {
                    // Remove the middle point of the three last
                    lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1]
                    lUpperSize--
                }
            }

            val lLower = Array(n) { DoubleArray(0) }

            lLower[0] = sorted[n - 1]
            lLower[1] = sorted[n - 2]

            var lLowerSize = 2

            for (i in n - 3 downTo 0) {
                lLower[lLowerSize] = sorted[i]
                lLowerSize++

                while (lLowerSize > 2 && !rightTurn(
                        lLower[lLowerSize - 3],
                        lLower[lLowerSize - 2],
                        lLower[lLowerSize - 1]
                    )
                ) {
                    // Remove the middle point of the three last
                    lLower[lLowerSize - 2] = lLower[lLowerSize - 1]
                    lLowerSize--
                }
            }

            val result = Array(lUpperSize + lLowerSize - 1) { DoubleArray(0) }
            var idx = 0
            for (i in 0 until lUpperSize) {
                result[idx] = lUpper[i]
                idx++
            }

            for (i in 1 until lLowerSize - 1) {
                // first and last coordinate are also part of lUpper; but polygon should end with itself
                result[idx] = lLower[i]
                idx++
            }
            // close the polygon
            result[result.size - 1] = result[0]
            return result
        }

        /**
         * @param a point
         * @param b point
         * @param c point
         * @return true if b is right of the line defined by a and c
         */
        @JvmStatic
        fun rightTurn(a: Point, b: Point, c: Point): Boolean {
            return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]) > 0
        }

        /**
         * Convert notation in degrees, minutes, and seconds to the decimal wgs 84 equivalent.
         * @param direction
         * n,s,e,w
         * @param degrees degrees
         * @param minutes minutes
         * @param seconds seconds
         * @return decimal degree
         */
        @JvmStatic
        fun toDecimalDegree(direction: String?, degrees: Double, minutes: Double, seconds: Double): Double {
            var factor = 1
            if (direction != null) {

                if (direction.startsWith("w") || direction.startsWith("s") || direction.startsWith("W") || direction.startsWith("S")) {
                    factor = -1
                }
            }
            return (degrees + minutes / 60 + seconds / 60.0 / 60.0) * factor
        }

        /**
         * @param point point
         * @return a json representation of the point
         */
        @JvmStatic
        fun toJson(point: DoubleArray): String {
            return if (point.size == 0) {
                "[]"
            } else {
                "[" + point[0] + ','.toString() + point[1] + "]"
            }
        }

        /**
         * @param points linestring
         * @return a json representation of the points
         */
        @JvmStatic
        fun toJson(points: Array<DoubleArray>): String {
            val buf = StringBuilder("[")
            for (i in points.indices) {
                buf.append(toJson(points[i]))
                if (i < points.size - 1) {
                    buf.append(',')
                }
            }
            buf.append("]")
            return buf.toString()
        }

        /**
         * @param points polygon
         * @return a json representation of the points
         */
        @JvmStatic
        fun toJson(points: Array<Array<DoubleArray>>): String {
            val buf = StringBuilder("[")
            for (i in points.indices) {
                buf.append(toJson(points[i]))
                if (i < points.size - 1) {
                    buf.append(',')
                }
            }
            buf.append("]")
            return buf.toString()
        }

        /**
         * @param points multipolygon
         * @return a json representation of the points
         */
        @JvmStatic
        fun toJson(points: Array<Array<Array<DoubleArray>>>): String {
            val buf = StringBuilder("[")
            for (i in points.indices) {
                buf.append(toJson(points[i]))
                if (i < points.size - 1) {
                    buf.append(',')
                }
            }
            buf.append("]")
            return buf.toString()
        }

        /**
         * Validates coordinates. Note. because of some edge cases at the extremes that I've encountered in several data sources, I've built in
         * a small tolerance for small rounding errors that allows e.g. 180.00000000000023 to validate.
         * @param latitude latitude between -90.0 and 90.0
         * @param longitude longitude between -180.0 and 180.0
         * @param strict if false, it will allow for small rounding errors. If true, it will not.
         * @throws IllegalArgumentException if the lat or lon is out of the allowed range.
         */
        @JvmStatic
        fun validate(latitude: Double, longitude: Double, strict: Boolean = false) {
            var roundedLat = latitude
            var roundedLon = longitude
            if (!strict) {
                // this gets rid of rounding errors in raw data e.g. 180.00000000000023 will validate
                roundedLat = (latitude * 1000000).roundToLong() / 1000000.0
                roundedLon = (longitude * 1000000).roundToLong() / 1000000.0
            }
            if (roundedLat < -90.0 || roundedLat > 90.0) {
                throw IllegalArgumentException("Latitude $latitude is outside legal range of -90,90")
            }
            if (roundedLon < -180.0 || roundedLon > 180.0) {
                throw IllegalArgumentException("Longitude $longitude is outside legal range of -180,180")
            }
        }

        /**
         * @param point point
         */
        @JvmStatic
        fun validate(point: DoubleArray) {
            validate(point[1], point[0], false)
        }

        /**
         * Calculate the approximate area. Like the distance, this is an approximation and you should account for an error
         * of about half a percent.
         *
         * @param polygon linestring
         * @return approximate area.
         */
        @JvmStatic
        fun area(polygon: Array<DoubleArray>): Double {
            if (polygon.size <= 3) throw IllegalArgumentException("polygon should have at least three elements")

            var total = 0.0
            var previous = polygon[0]

            val center = polygonCenter(*polygon)
            val xRef = center[0]
            val yRef = center[1]

            for (i in 1 until polygon.size) {
                val current = polygon[i]
                // convert to cartesian coordinates in meters, note this not very exact
                val x1 = (previous[0] - xRef) * (6378137 * PI / 180) * cos(yRef * PI / 180)
                val y1 = (previous[1] - yRef) * toRadians(6378137.0)
                val x2 = (current[0] - xRef) * (6378137 * PI / 180) * cos(yRef * PI / 180)
                val y2 = (current[1] - yRef) * toRadians(6378137.0)

                // calculate crossproduct
                total += x1 * y2 - x2 * y1
                previous = current
            }

            return 0.5 * abs(total)
        }

        /**
         * @param bbox bounding box of [lat,lat,lon,lon]
         * @return the area of the bounding box
         */
        @JvmStatic
        fun area(bbox: DoubleArray): Double {
            if (bbox.size != 4) {
                throw IllegalArgumentException("Boundingbox should be array of [minLat, maxLat, minLon, maxLon]")
            }

            val latDist = distance(bbox[0], bbox[2], bbox[1], bbox[2])
            val lonDist = distance(bbox[0], bbox[2], bbox[0], bbox[3])

            return latDist * lonDist
        }

        /**
         * Calculate area of polygon with holes. Assumes geojson style notation where the first 2d array is the outer
         * polygon and the rest represents the holes.
         *
         * @param polygon polygon
         * @return area covered by the outer polygon
         */
        @JvmStatic
        fun area(polygon: Array<Array<DoubleArray>>): Double {
            if (polygon.size <= 0) throw IllegalArgumentException("array should not be empty")
            var area = area(polygon[0])
            for (i in 1 until polygon.size) {
                // subtract the holes
                area = area - area(polygon[i])
            }
            return area
        }

        /**
         * Calculate area of a multi polygon.
         * @param multiPolygon geojson style multi polygon
         * @return area of the outer polygons
         */
        @JvmStatic
        fun area(multiPolygon: Array<Array<Array<DoubleArray>>>): Double {
            var area = 0.0
            for (doubles in multiPolygon) {
                area += area(doubles)
            }
            return area
        }

        /**
         * @param p point
         * @return "(longitude,latitude)"
         */
        @JvmStatic
        fun pointToString(p: DoubleArray): String {
            return "(" + p[0] + "," + p[1] + ")"
        }

        /**
         * @param line line
         * @return "(x1,y1),(x2,y2),..."
         */
        @JvmStatic
        fun lineToString(line: Array<DoubleArray>): String {
            val buf = StringBuilder()
            for (p in line) {
                buf.append(pointToString(p)).append(",")
            }
            buf.setLength(buf.length - 1)
            return buf.toString()
        }

        /**
         * Implementation of Douglas Peucker algorithm to simplify multi polygons.
         *
         * Simplifies multi polygon with lots of segments by throwing out in between points with a perpendicular
         * distance of less than the tolerance (in meters). Implemented using the Douglas Peucker algorithm:
         * http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
         * @param points multi polygon
         * @param tolerance tolerance in meters
         * @return a simpler multi polygon
         */
        @JvmStatic
        fun simplifyMultiPolygon(
            points: Array<Array<Array<DoubleArray>>>,
            tolerance: Double
        ): Array<Array<Array<DoubleArray>>> {
            val result = Array(points.size) { Array(0) { Array(0) { DoubleArray(0) } } }
            var i = 0
            for (polygon in points) {
                result[i++] = simplifyPolygon(polygon, tolerance)
            }
            return result
        }

        /**
         * Implementation of Douglas Peucker algorithm to simplify polygons.
         *
         * Simplifies polygon with lots of segments by throwing out in between points with a perpendicular
         * distance of less than the tolerance (in meters). Implemented using the Douglas Peucker algorithm:
         * http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
         * @param points a 3d array with the outer and inner polygons.
         * @param tolerance tolerance in meters
         * @return a simpler polygon
         */
        @JvmStatic
        fun simplifyPolygon(points: Array<Array<DoubleArray>>, tolerance: Double): Array<Array<DoubleArray>> {
            val result = Array(points.size) { Array(0) { DoubleArray(0) } }
            var i = 0
            for (line in points) {
                result[i++] = simplifyLine(line, tolerance)
            }
            return result
        }

        /**
         * Implementation of Douglas Peucker algorithm to simplify lines.
         *
         * Simplifies lines and polygons with lots of segments by throwing out in between points with a perpendicular
         * distance of less than the tolerance (in meters). Implemented using the Douglas Peucker algorithm:
         * http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
         * @param points a 2d array that may be either a line or a polygon.
         * @param tolerance tolerance in meters
         * @return a simpler line
         */
        @JvmStatic
        fun simplifyLine(points: Array<DoubleArray>, tolerance: Double): Array<DoubleArray> {
            var dmax = 0.0
            var index = 0
            if (points.size == 3) {
                dmax = distance(points[0], points[points.size - 1], points[1]) // edge case
            }

            for (i in 2 until points.size - 1) {
                val d = distance(points[0], points[points.size - 1], points[i])
                if (d > dmax) {
                    index = i
                    dmax = d
                }
            }
            if (dmax > tolerance && points.size > 3) {
                val leftArray = Array(index) { DoubleArray(0) }
                System.arraycopy(points, 0, leftArray, 0, index)

                val left = simplifyLine(leftArray, tolerance)

                val rightArray = Array(points.size - index) { DoubleArray(0) }
                System.arraycopy(points, index, rightArray, 0, points.size - index)

                val right = simplifyLine(rightArray, tolerance)
                val result = Array(left.size + right.size) { DoubleArray(0) }
                System.arraycopy(left, 0, result, 0, left.size)
                System.arraycopy(right, 0, result, left.size, right.size)
                return result
            } else if (dmax > tolerance && points.size <= 3) {
                return points
            } else {
                val simplified = arrayOf(points[0], points[points.size - 1])
                return if (points.size > 2) {
                    simplified
                } else {
                    points
                }
            }
        }
    }
}
