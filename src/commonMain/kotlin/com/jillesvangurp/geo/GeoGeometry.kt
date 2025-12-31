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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.jillesvangurp.geo

import com.jillesvangurp.geojson.*
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.math.*


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
        // this value is approximated to account for the fact that the world is not a perfect sphere
        const val EARTH_RADIUS_METERS = 6371000.0
        const val WGS84_RADIUS = 6378137
        const val EARTH_CIRCUMFERENCE_METERS = EARTH_RADIUS_METERS * PI * 2.0
        const val DEGREE_LATITUDE_METERS = EARTH_RADIUS_METERS * PI / 180.0
        const val DEGREES_TO_RADIANS =  PI / 180.0
        const val RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS


        /**
         * @param pointCoordinates point
         * @return bounding box that contains the point as a double array of
         * [lat,lat,lon,lon}
         */
        fun boundingBox(pointCoordinates: PointCoordinates): BoundingBox {
            return doubleArrayOf(pointCoordinates.longitude, pointCoordinates.latitude, pointCoordinates.longitude, pointCoordinates.latitude)
        }

        /**
         * @param points line
         * @return bounding box that contains the lineString as a double array of
         * [minLat,maxLat,minLon,maxLon}
         */
        fun boundingBox(points: MultiPointCoordinates): BoundingBox {
            var southLat = Int.MAX_VALUE.toDouble()
            var westLon = Int.MAX_VALUE.toDouble()
            var northLat = Int.MIN_VALUE.toDouble()
            var eastLon = Int.MIN_VALUE.toDouble()

            for (p in points) {
                southLat = min(southLat, p.latitude)
                westLon = min(westLon, p.longitude)
                northLat = max(northLat, p.latitude)
                eastLon = max(eastLon, p.longitude)
            }

            val bbox = doubleArrayOf(westLon, southLat, eastLon, northLat)
            return bbox
        }

        /**
         * @param polygonCoordinates 3d polygon array
         * @return bounding box that contains the polygon as a double array of
         * [minLat,maxLat,minLon,maxLon}
         */
        fun boundingBox(polygonCoordinates: PolygonCoordinates): BoundingBox {
            var westLon = Int.MAX_VALUE.toDouble()
            var southLat = Int.MAX_VALUE.toDouble()

            var eastLon = Int.MIN_VALUE.toDouble()
            var northLat = Int.MIN_VALUE.toDouble()
            for (linearRing in polygonCoordinates) {
                for (point in linearRing) {
                    westLon = min(westLon, point.longitude)
                    southLat = min(southLat, point.latitude)

                    eastLon = max(eastLon, point.longitude)
                    northLat = max(northLat, point.latitude)
                }
            }
            val bbox = doubleArrayOf(westLon, southLat, eastLon, northLat)
            return bbox // return doubleArrayOf(southLat, northLat, westLon, eastLon)
        }

        /**
         * @param multiPolygonCoordinates 4d multipolygon array
         * @return bounding box that contains the multiPolygon as a double array of
         * [minLat,maxLat,minLon,maxLon}
         */
        fun boundingBox(multiPolygonCoordinates: MultiPolygonCoordinates): BoundingBox {
            var southLat = Int.MAX_VALUE.toDouble()
            var westLon = Int.MAX_VALUE.toDouble()
            var northLat = Int.MIN_VALUE.toDouble()
            var eastLon = Int.MIN_VALUE.toDouble()
            for (polygon in multiPolygonCoordinates) {
                for (linearRing in polygon) {
                    for (point in linearRing) {
                        southLat = min(southLat, point[1])
                        westLon = min(westLon, point[0])
                        northLat = max(northLat, point[1])
                        eastLon = max(eastLon, point[0])
                    }
                }
            }
            val bbox = doubleArrayOf(westLon, southLat, eastLon, northLat)
            return bbox // return doubleArrayOf(southLat, northLat, westLon, eastLon)
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
        fun filterNoiseFromPointCloud(points: MultiPointCoordinates, percentage: Float): MultiPointCoordinates {

            points.sortWith { p1, p2 ->
                when {
                    p1[0] == p2[0] -> p1[1].compareTo(p2[1])
                    p1[0] > p2[0] -> 1
                    p1[0] == p2[0] -> 0
                    else -> -1
                }
            }

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
        fun bboxContains(bbox: BoundingBox, latitude: Double, longitude: Double): Boolean {
            validate(latitude, longitude, false)
            return bbox.southLatitude <= latitude && latitude <= bbox.northLatitude && bbox.westLongitude <= longitude && longitude <= bbox.eastLongitude
        }

        /**
         * Determine whether a point is contained in a polygon. Note, technically
         * the points that make up the polygon are not contained by it.
         *
         * @param pointCoordinates point
         * @param polygonCoordinatesPoints 3d array representing a geojson polygon. Note. the polygon holes are ignored currently.
         * @return true if the polygon contains the coordinate
         */
        fun polygonContains(pointCoordinates: PointCoordinates, polygonCoordinatesPoints: PolygonCoordinates): Boolean {
            validate(pointCoordinates)
            return polygonContains(pointCoordinates[1], pointCoordinates[0], *polygonCoordinatesPoints[0])
        }

        fun polygonContains(
            pointCoordinates: PointCoordinates,
            polygonCoordinatesPoints: LinearRingCoordinates
        ): Boolean {
            validate(pointCoordinates)
            return polygonContains(pointCoordinates[1], pointCoordinates[0], *polygonCoordinatesPoints)
        }

        /**
         * Determine whether a point is contained in a polygon. Note, technically
         * the points that make up the polygon are not contained by it.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param polygonCoordinatesPoints 3d array representing a geojson polygon. Note. the polygon holes are ignored currently.
         * @return true if the polygon contains the coordinate
         */
        fun polygonContains(
            latitude: Double,
            longitude: Double,
            polygonCoordinatesPoints: PolygonCoordinates
        ): Boolean {
            validate(latitude, longitude, false)
            return polygonContains(latitude, longitude, *polygonCoordinatesPoints[0])
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
        fun polygonContains(
            latitude: Double,
            longitude: Double,
            vararg polygonPoints: PointCoordinates
        ): Boolean {
            fun wrapLongitude(diff: Double): Double = when {
                diff > 180 -> diff - 360
                diff < -180 -> diff + 360
                else -> diff
            }

            validate(latitude, longitude, false)
            require(polygonPoints.size >= 3) { "a polygon must have at least three points" }

            // used for rounding errors
            val epsilon = 1e-9

            // Normalize longitudes around test point
            val norm = polygonPoints.map {
                doubleArrayOf(wrapLongitude(it[0] - longitude), it[1])
            }.toTypedArray()

            // Early-out bbox check
            if (!bboxContains(boundingBox(norm), latitude, 0.0)) return false

            // Direct vertex match
            if (polygonPoints.any { abs(it.latitude - latitude) < epsilon && abs(it.longitude - longitude) < epsilon }) {
                return true
            }

            // Edge match
            for (i in norm.indices) {
                val (x1, y1) = norm[i]
                val (x2, y2) = norm[(i + 1) % norm.size]
                if ((x1 != x2 || y1 != y2) && onSegment(0.0, latitude, x1, y1, x2, y2)) {
                    return true
                }
            }

            // Ray-cast eastward from (0, latitude)
            var hits = 0
            for (i in norm.indices) {
                val (x1, y1) = norm[i]
                val (x2, y2) = norm[(i + 1) % norm.size]

                // Ignore horizontal segments
                if (abs(y1 - y2) < epsilon) continue

                // Handle crossing the horizontal ray
                val lower = if (y1 < y2) y1 else y2
                val upper = if (y1 < y2) y2 else y1
                if (latitude > lower && latitude <= upper) {
                    val xInt = x1 + (latitude - y1) * (x2 - x1) / (y2 - y1)
                    if (xInt > 0) hits++
                }
            }

            return hits % 2 == 1
        }

        /**
         * Simple rounding method that allows you to get rid of some decimals in a
         * double.
         *
         * @param d a double
         * @param decimals the number of desired decimals after the dot
         * @return d rounded to the specified precision
         */
        fun roundToDecimals(d: Double, decimals: Int): Double {
            if (decimals > 17) {
                throw IllegalArgumentException("this probably doesn't do what you want; makes sense only for <= 17 decimals")
            }
            val factor = 10.0.pow(decimals.toDouble())
            return (d * factor).roundToLong() / factor
        }

        fun Double.roundDecimals(decimals:Int) = roundToDecimals(this,decimals)

        fun linesCross(
            l1p1: PointCoordinates,
            l1p2: PointCoordinates,
            l2p1: PointCoordinates,
            l2p2: PointCoordinates
        ): Boolean {
            return linesCross(
                l1p1.longitude,
                l1p1.latitude,
                l1p2.longitude,
                l1p2.latitude,
                l2p1.longitude,
                l2p1.latitude,
                l2p2.longitude,
                l2p2.latitude
            )
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
                        val l1Min = min(y1, y2)
                        val l1Max = max(y1, y2)
                        val l2Min = min(v1, v2)
                        val l2Max = max(v1, v2)
                        l1Min <= l2Max && l2Min <= l1Max
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
        fun translateLongitude(latitude: Double, longitude: Double, meters: Double): PointCoordinates {
            validate(latitude, longitude, false)
            return doubleArrayOf(
                longitude + meters / lengthOfLongitudeDegreeAtLatitude(latitude),
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
        fun translateLatitude(latitude: Double, longitude: Double, meters: Double): PointCoordinates {
            return doubleArrayOf(longitude, latitude + meters / DEGREE_LATITUDE_METERS)
        }

        /**
         * Translate a point by the specified meters along the longitude and
         * latitude. Note, this method assumes the earth is a sphere and the result
         * is not going to be very precise for larger distances.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param latitudinalMeters distance in meters that the point should be translated along the latitude
         * @param longitudinalMeters distance in meters that the point should be translated along the longitude
         * @return the translated coordinate.
         */
        fun translate(
            latitude: Double,
            longitude: Double,
            latitudinalMeters: Double,
            longitudinalMeters: Double
        ): DoubleArray {
            validate(latitude, longitude, false)
            val longitudal = translateLongitude(latitude, longitude, longitudinalMeters)
            return translateLatitude(longitudal.latitude, longitudal.longitude, latitudinalMeters)
        }

        fun translate(
            point: PointCoordinates,
            xMeters: Double,
            yMeters: Double
        ): DoubleArray = translate(point.latitude,point.longitude, yMeters, xMeters)


        /**
         * Calculate a bounding box of the specified longitudinal and latitudinal meters with the latitude/longitude as the center.
         * @param latitude latitude
         * @param longitude longitude
         * @param latitudinalMeters distance in meters that the point should be translated along the latitude
         * @param longitudinalMeters distance in meters that the point should be translated along the longitude
         * @return [maxlat,minlat,maxlon,minlon]
         */
        fun rectangleAroundCentroid(latitude: Double, longitude: Double, latitudinalMeters: Double, longitudinalMeters: Double): DoubleArray {
            validate(latitude, longitude, false)

            val topRight = translate(latitude, longitude, latitudinalMeters / 2, longitudinalMeters / 2)
            val bottomLeft = translate(latitude, longitude, -latitudinalMeters / 2, -longitudinalMeters / 2)

            val southLat = bottomLeft[1]
            val northLat = topRight[1]
            val westLon = bottomLeft[0]
            val eastLon = topRight[0]
            return doubleArrayOf(westLon, southLat, eastLon, northLat)
        }

        fun calculateTileBboxesForBoundingBox(bbox: BoundingBox, height: Int = 512, width: Int = 512, minZoom: Double = 22.0): List<BoundingBox> {
            val zoomLevel = bbox.zoomLevel(height, width, minZoom)
            val factor = 2.0.pow(zoomLevel)

            val longitudalGridAngle = 360.0 / factor
            val latitudalGridAngle = 180.0 / factor
            val mostWest = bbox.bottomLeft.longitude - bbox.bottomLeft.longitude % longitudalGridAngle

            var lat = bbox.southLatitude - bbox.southLatitude % latitudalGridAngle
            val cells = mutableListOf<BoundingBox>()
            while (lat < bbox.northLatitude) {
                var lon = mostWest
                while (lon < bbox.eastLongitude) {
                    cells.add(doubleArrayOf(lon, lat, lon + longitudalGridAngle, lat + latitudalGridAngle))
                    lon += longitudalGridAngle
                }
                lat += latitudalGridAngle
            }
            return cells
        }

        fun toRadians(degrees: Double): Double {
            return degrees * DEGREES_TO_RADIANS
        }

        fun fromRadians(radians: Double): Double {
            return radians * RADIANS_TO_DEGREES
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
        fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
            validate(lat1, long1, false)
            validate(lat2, long2, false)

            val deltaLat = toRadians(lat2 - lat1)
            val deltaLon = toRadians(long2 - long1)

            val a =
                sin(deltaLat / 2) * sin(deltaLat / 2) + cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(deltaLon / 2) * sin(
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
        fun distance(p1: PointCoordinates, p2: PointCoordinates): Double {
            return distance(p1[1], p1[0], p2[1], p2[0])
        }

        /**
         * Cheap but not very precise over larger distances
         */
        fun equirectangularDistance(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val x = toRadians(lon2 - lon1) * cos(toRadians((lat1 + lat2) / 2))
            val y = toRadians(lat2 - lat1)
            return sqrt(x * x + y * y) * EARTH_RADIUS_METERS
        }

        fun equirectangularDistance(p1: PointCoordinates, p2: PointCoordinates): Double =
            equirectangularDistance(p1[1], p1[0], p2[1], p2[0])

        /**
         * Calculate distance in meters using the Vicenty algorithm.
         *
         * This provides better accuracy than Haversine as it takes into account the flattening of the earth.
         * Note, this is still an approximation of course. Over short distances, there should not be any difference.
         */
        fun vicentyDistance(p1: PointCoordinates, p2: PointCoordinates) = vincenty(p1, p2).distance

        /**
         * Calculate distance in meters using the Vicenty algorithm.
         *
         * This provides better accuracy than Haversine as it takes into account the flattening of the earth.
         * Note, this is still an approximation of course. Over short distances, there should not be any difference.
         */
        fun vicentyDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) = vicentyDistance(
            doubleArrayOf(lon1, lat1),
            doubleArrayOf(lon2, lat2)
        )

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
        fun distanceToLine(l1: PointCoordinates, l2: PointCoordinates, p: PointCoordinates): Double {
            return distance(l1[1], l1[0], l2[1], l2[0], p[1], p[0])
        }

        /**
         * @param pointCoordinates point
         * @param lineStringCoordinates linestring
         * @return the distance of the point to the line
         */
        fun distanceToLineString(
            pointCoordinates: PointCoordinates,
            lineStringCoordinates: LineStringCoordinates
        ): Double {
            if (lineStringCoordinates.size < 2) {
                throw IllegalArgumentException("not enough segments in line")
            }
            var minDistance = Double.MAX_VALUE
            var last = lineStringCoordinates[0]
            for (i in 1 until lineStringCoordinates.size) {
                val current = lineStringCoordinates[i]
                val distance = distanceToLine(last, current, pointCoordinates)
                minDistance = min(minDistance, distance)
                last = current
            }
            return minDistance
        }

        /**
         * @param pointCoordinates point
         * @param polygon 2d linestring that is a polygon
         * @return distance to polygon
         */
        fun distanceToPolygon(pointCoordinates: PointCoordinates, polygon: LinearRingCoordinates): Double {
            if (polygon.size < 3) {
                throw IllegalArgumentException("not enough segments in polygon")
            }
            return if (polygonContains(pointCoordinates[1], pointCoordinates[0], *polygon)) {
                0.0
            } else distanceToLineString(pointCoordinates, polygon)
        }

        /**
         * @param pointCoordinates point
         * @param polygonCoordinates polygon
         * @return distance to polygon
         */
        fun distanceToPolygon(pointCoordinates: PointCoordinates, polygonCoordinates: PolygonCoordinates): Double {
            if (polygonCoordinates.isEmpty()) {
                throw IllegalArgumentException("empty polygon")
            }
            return distanceToPolygon(pointCoordinates, polygonCoordinates[0])
        }

        /**
         * @param point point
         * @param multiPolygon multi polygon
         * @return distance to the nearest of the polygons in the multipolygon
         */
        fun distanceToMultiPolygon(point: DoubleArray, multiPolygon: Array<Array<Array<DoubleArray>>>): Double {
            var distance = Double.MAX_VALUE
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
        fun polygonCenter(vararg polygonPoints: DoubleArray): DoubleArray {
            var minLon = 180.0
            var maxLon = -180.0
            var minLat = 90.0
            var maxLat = -90.0
            for (coordinate in polygonPoints) {
                val lon = coordinate.longitude
                if(lon<minLon) minLon = lon
                if(lon>maxLon) maxLon = lon
                val lat = coordinate.latitude
                if(lat<minLat) minLat = lat
                if(lat>maxLat) maxLat = lat

            }
            return doubleArrayOf((minLon+maxLon)/2.0, (minLat+maxLat)/2.0)
        }

        /**
         * Converts a circle to a polygon.
         * This method does not behave very well very close to the poles because the math gets a little funny there.
         *
         * @param segments
         * number of segments the polygon should have. The higher this
         * number, the better of an approximation the polygon is for the
         * circle.
         * @param centerLat latitude
         * @param centerLon longitude
         * @param radius radius of the circle
         * @return a linestring polygon
         */
        fun circle2polygon(segments: Int, centerLat: Double, centerLon: Double, radius: Double): PolygonCoordinates {
            validate(centerLat, centerLon, false)

            if (segments < 3) {
                throw IllegalArgumentException("you need a minimum of 5 segments")
            }
            val points = mutableListOf<PointCoordinates>()

            val relativeLatitude = radius / EARTH_RADIUS_METERS * 180 / PI

            // things get funny near the north and south pole, so doing a modulo 90
            // to ensure that the relative amount of degrees doesn't get too crazy.
            val relativeLongitude = relativeLatitude / cos(toRadians(centerLat)) % 90

            for (i in 0 until segments) {
                // radians go from 0 to 2*PI; we want to divide the circle in nice
                // segments
                var theta = 2.0 * PI * i.toDouble() / segments
                // trying to avoid theta being exact factors of pi because that results in some funny behavior around the
                // north-pole
                theta += 0.001
                if (theta >= 2 * PI) {
                    theta -= 2 * PI
                }

                // on the unit circle, any point of the circle has the coordinate
                // cos(t),sin(t) where t is the radian. So, all we need to do that
                // is multiply that with the relative latitude and longitude
                // note, latitude takes the role of y, not x. By convention we
                // always note latitude, longitude instead of the other way around
                var latOnCircle = centerLat + relativeLatitude * sin(theta)
                var lonOnCircle = centerLon + relativeLongitude * cos(theta)
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

                points.add(doubleArrayOf(lonOnCircle, latOnCircle))
            }
            // should end with same point as the origin
            points.add(points[0].let { doubleArrayOf(it.longitude,it.latitude) })
            return arrayOf(points.toTypedArray())
        }

        /**
         * Rotates a [point] around an [anchor] by [degrees]. Returns rotated point.
         *
         * Note, rotation is counter clockwise. That's just how the math works. Negate [degrees] for clockwise.
         */
        fun rotateAround(anchor: PointCoordinates, point: PointCoordinates, degrees: Double): PointCoordinates {

            // distances are absolute, so calculate the direction we need to translate to
            val x = distance(anchor, doubleArrayOf(point.x, anchor.y)).let {
                if(anchor.x>point.x) {
                    -it
                } else {
                    it
                }
            }
            val y = distance(anchor, doubleArrayOf(anchor.x, point.y)).let {
                if(anchor.y>point.y) {
                    -it
                } else {
                    it
                }
            }

            val radians = toRadians(degrees)

            val newX = x*cos(radians) - y*sin(radians)
            val newY = x*sin(radians) + y*cos(radians)

            return translate(anchor,newX,newY)
        }

        /**
         * @param left a 2d array representing a polygon
         * @param right a 2d array representing a polygon
         * @return true if the two polygons overlap. Important: this only works for simple convex polygons. Overlap for concave polygons is more complicated.
         */
        fun overlap(left: LinearRingCoordinates, right: LinearRingCoordinates): Boolean {
            if (polygonContains(polygonCenter(*right), left) || polygonContains(polygonCenter(*left), right)) {
                return true
            }

            for (p in right) {
                if (polygonContains(p, left)) {
                    return true
                }
            }
            for (p in left) {
                if (polygonContains(p, right)) {
                    return true
                }
            }

            for (i in 0 until left.size - 1) {
                val l1p1 = left[i]
                val l1p2 = left[i + 1]
                for (j in 0 until right.size - 1) {
                    val l2p1 = right[j]
                    val l2p2 = right[j + 1]

                    if (linesCross(l1p1, l1p2, l2p1, l2p2)) {
                        return true
                    }
                }
            }
            return false
        }

        fun overlap(left: PolygonCoordinates, right: PolygonCoordinates) = overlap(left[0], right[0])

        /**
         * @param containingPolygon linestring polygon
         * @param containedPolygon linestring polygon
         * @return true if the containing polygon fully contains the contained polygon. Important: this only works for simple convex polygons. Contains for concave polygons is more complicated. https://en.wikipedia.org/wiki/Point_in_polygon
         */
        fun contains(containingPolygon: LinearRingCoordinates, containedPolygon: LinearRingCoordinates): Boolean {
            for (p in containedPolygon) {
                if (!polygonContains(p[1], p[0], *containingPolygon)) {
                    return false
                }
            }
            return true
        }

        /**
         * Attempts to expand the linearRing by calculating points around each of the polygon points that are translated the
         * specified amount of meters away. A new polygon is constructed from the resulting point cloud.
         *
         * Given that the contains algorithm disregards linearRing points as not contained in the polygon, it is useful to
         * expand the linearRing a little if you do require this.
         *
         * @param meters distance
         * @param points linestring polygon
         * @return a new linearRing that fully contains the old polygon and is roughly the specified meters wider.
         */
        fun expandPolygon(meters: Double, points: LinearRingCoordinates): LinearRingCoordinates {
            val expanded = Array(points.size * 8) { DoubleArray(0) }
            // generate eight variants of each point
            for (i in points.indices) {
                val p = points[i]
                val lonPos = translateLongitude(p.latitude, p.longitude, meters)[0]
                val lonNeg = translateLongitude(p.latitude, p.longitude, (-1 * meters))[0]
                val latPos = translateLatitude(p.latitude, p.longitude, meters)[1]
                val latNeg = translateLatitude(p.latitude, p.longitude, (-1 * meters))[1]
                expanded[i * 8] = doubleArrayOf(lonPos, latPos)
                expanded[i * 8 + 1] = doubleArrayOf(lonPos, latNeg)
                expanded[i * 8 + 4] = doubleArrayOf(lonPos, p.latitude)
                expanded[i * 8 + 2] = doubleArrayOf(lonNeg, latPos)
                expanded[i * 8 + 3] = doubleArrayOf(lonNeg, latNeg)
                expanded[i * 8 + 5] = doubleArrayOf(lonNeg, p.latitude)

                expanded[i * 8 + 6] = doubleArrayOf(p.longitude, latPos)
                expanded[i * 8 + 7] = doubleArrayOf(p.longitude, latNeg)
            }
            // create a polygon that surrounds all the points
            return polygonForPoints(expanded)
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
        fun expandPolygon(meters: Double, points: PolygonCoordinates): PolygonCoordinates {
            return arrayOf(expandPolygon(meters, points[0]))
        }

        /**
         * Calculate a polygon for the specified points.
         * @param points linestring polygon
         * @return a convex polygon for the points
         */
        fun polygonForPoints(points: MultiPointCoordinates): LinearRingCoordinates {
            if (points.size < 3) {
                throw IllegalStateException("need at least 3 pois for a polygon")
            }

            val sorted = points.copyOf()
            sorted.sortWith { p1, p2 ->
                when {
                    p1[0] == p2[0] -> p1[1].compareTo(p2[1])
                    else -> p1[0].compareTo(p2[0])
                }
            }

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
        fun rightTurn(a: PointCoordinates, b: PointCoordinates, c: PointCoordinates): Boolean {
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
        fun toDecimalDegree(direction: String?, degrees: Int, minutes: Int, seconds: Double): Double {
            var factor = 1
            if (direction != null) {

                if (direction.startsWith("w") || direction.startsWith("s") || direction.startsWith("W") || direction.startsWith(
                        "S"
                    )
                ) {
                    factor = -1
                }
            }
            return (degrees.toDouble() + minutes.toDouble() / 60.0 + seconds / 60.0 / 60.0) * factor
        }

        private fun mod(x: Double, m: Double): Double {
            return (x % m + m) % m
        }

        fun wrap(n: Double, min: Double, max: Double): Double {
            return if (n >= min && n < max) n else mod(n - min, max - min) + min
        }

        /**
         * Returns the heading from one LatLng to another LatLng as a compass direction.
         *
         * [stackoverflow(]https://stackoverflow.com/questions/9457988/bearing-from-one-coordinate-to-another)
         *
         * @return The heading in degrees clockwise from north.
         */
        fun headingFromTwoPoints(from: PointCoordinates, to: PointCoordinates): Double = headingFromTwoPoints(from.latitude,from.longitude,to.latitude,to.longitude)

        /**
         * Returns the heading from one LatLng to another LatLng as a compass direction.
         *
         * [stackoverflow](https://stackoverflow.com/questions/9457988/bearing-from-one-coordinate-to-another)
         *
         * @return The heading in degrees clockwise from north.
         */
        fun headingFromTwoPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val latitude1 = toRadians(lat1)
            val latitude2 = toRadians(lat2)
            val longDiff = toRadians(lon2 - lon1)
            val y = sin(longDiff) * cos(latitude2)
            val x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(
                    latitude2
                ) * cos(longDiff)
            return (fromRadians(atan2(y, x)) + 360) % 360
        }
        /**
         * @param point point
         * @return a json representation of the point
         */
        fun toJson(point: DoubleArray): String {
            return if (point.isEmpty()) {
                "[]"
            } else {
                "[" + point[0] + ','.toString() + point[1] + "]"
            }
        }

        /**
         * @param points linestring
         * @return a json representation of the points
         */
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
        fun validate(latitude: Double, longitude: Double, strict: Boolean = false) {
            // causing issues with latest kotlin js 1.8
            val minLat = if(strict) -90.0 else -90.0002
            val maxLat = if(strict) 90.0 else 90.0002
            val minLon = if(strict) -180.0 else -180.0002
            val maxLon = if(strict) 180.0 else 180.0002
            if (latitude < minLat || latitude > maxLat) {
                throw IllegalArgumentException("Latitude $latitude is outside legal range of -90,90")
            }
            if (longitude < minLon || longitude > maxLon) {
                throw IllegalArgumentException("Longitude $longitude is outside legal range of -180,180")
            }
        }

        /**
         * @param pointCoordinates point
         */
        fun validate(pointCoordinates: PointCoordinates) {
            validate(pointCoordinates[1], pointCoordinates[0], false)
        }

        /**
         * Calculate the approximate area. Like the distance, this is an approximation and you should account for an error
         * of about half a percent.
         *
         * @param polygon linestring
         * @return approximate area.
         */
        fun area(polygon: LinearRingCoordinates): Double {
            if (polygon.size <= 3) throw IllegalArgumentException("polygon should have at least three elements")

            var total = 0.0
            var previous = polygon[0]

            val center = polygonCenter(*polygon)
            val xRef = center[0]
            val yRef = center[1]

            for (i in 1 until polygon.size) {
                val current = polygon[i]
                // convert to cartesian coordinates in meters, note this not very exact
                val x1 = (previous[0] - xRef) * (WGS84_RADIUS * PI / 180) * cos(yRef * PI / 180)
                val y1 = (previous[1] - yRef) * toRadians(6378137.0)
                val x2 = (current[0] - xRef) * (WGS84_RADIUS * PI / 180) * cos(yRef * PI / 180)
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
        fun area(bbox: BoundingBox): Double {
            if (bbox.size != 4) {
                throw IllegalArgumentException("Boundingbox should be array of [minLat, maxLat, minLon, maxLon]")
            }
            val latDist = distance(bbox.southLatitude, bbox.westLongitude, bbox.northLatitude, bbox.westLongitude)
            val lonDist = distance(bbox.northLatitude, bbox.westLongitude, bbox.northLatitude, bbox.eastLongitude)
            return latDist * lonDist
        }

        /**
         * Calculate area of polygon with holes. Assumes geojson style notation where the first 2d array is the outer
         * polygon and the rest represents the holes.
         *
         * @param polygonCoordinates polygon
         * @return area covered by the outer polygon
         */
        fun area(polygonCoordinates: PolygonCoordinates): Double {
            if (polygonCoordinates.isEmpty()) throw IllegalArgumentException("array should not be empty")
            var area = area(polygonCoordinates[0])
            for (i in 1 until polygonCoordinates.size) {
                // subtract the holes
                area = area - area(polygonCoordinates[i])
            }
            return area
        }

        /**
         * Calculate area of a multi polygon.
         * @param multiPolygonCoordinates geojson style multi polygon
         * @return area of the outer polygons
         */
        fun area(multiPolygonCoordinates: MultiPolygonCoordinates): Double {
            var area = 0.0
            for (doubles in multiPolygonCoordinates) {
                area += area(doubles)
            }
            return area
        }

        /**
         * @param p point
         * @return "(longitude,latitude)"
         */
        fun pointToString(p: PointCoordinates): String {
            return "(" + p[0] + "," + p[1] + ")"
        }

        /**
         * @param lineCoordinates line
         * @return "(x1,y1),(x2,y2),..."
         */
        fun lineToString(lineCoordinates: LineStringCoordinates): String {
            val buf = StringBuilder()
            for (p in lineCoordinates) {
                buf.append(pointToString(p)).append(",")
            }
            return buf.toString().substring(0, buf.length - 1)
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
        fun simplifyMultiPolygon(
            points: MultiPolygonCoordinates,
            tolerance: Double
        ): MultiPolygonCoordinates {
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
        fun simplifyPolygon(points: PolygonCoordinates, tolerance: Double): PolygonCoordinates {
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
        fun simplifyLine(points: LinearRingCoordinates, tolerance: Double): LinearRingCoordinates {
            var dmax = 0.0
            var index = 0
            if (points.size == 3) {
                dmax = distanceToLine(points[0], points[points.size - 1], points[1]) // edge case
            }

            for (i in 2 until points.size - 1) {
                val d = distanceToLine(points[0], points[points.size - 1], points[i])
                if (d > dmax) {
                    index = i
                    dmax = d
                }
            }
            if (dmax > tolerance && points.size > 3) {

                val leftArray = points.copyOfRange(0, index)

                val leftSimlified = simplifyLine(leftArray, tolerance)

                val rightArray = points.copyOfRange(index, points.size - 1)

                val rightSimplified = simplifyLine(rightArray, tolerance)
                return leftSimlified + rightSimplified
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

        /**
         * Checks if the polygon follows the geojson right hand side rule that requires
         * the outer linear ring to have the coordinates in counter clockwise order (right hand)
         * and the inner ones in clockwise order.
         *
         * Returns a copy of the polygon with the outer ring and inner rings corrected.
         *
         * https://tools.ietf.org/html/rfc7946#section-3.1.6
         */
        fun PolygonCoordinates.ensureFollowsRightHandSideRule(): PolygonCoordinates {
            val holes = holes()
            if (this.isValid()) {
                return this
            } else {
                val newPolygonCoordinates = listOf(
                    outer().let {
                        if (it.isCounterClockWise()) {
                            it
                        } else {
                            it.changeOrder()
                        }
                    }
                )
                val inner = holes.map { it ->
                    it.let { hole ->
                        if (hole.isClockWise()) {
                            hole
                        } else {
                            hole.changeOrder()
                        }
                    }
                }
                return (newPolygonCoordinates + inner).toTypedArray()
            }
        }

        /**
         * Applies the right hand rule to all of the polygons. Returns a corrected copy.
         *
         * https://tools.ietf.org/html/rfc7946#section-3.1.6
         */
        fun MultiPolygonCoordinates.ensureFollowsRightHandSideRule() =
            this.map { it.ensureFollowsRightHandSideRule() }.toTypedArray()

        /**
         * True if start and end are the same.
         */
        fun LinearRingCoordinates.hasSameStartAndEnd() = this.first().contentEquals(this.last()) && this.size > 1

        /**
         * Returns the first linear ring, which is the outer ring of the polygon. The remaining rings are holes.
         */
        fun PolygonCoordinates.outer() = this[0]

        /**
         * Return the holes in the polygon.
         */
        fun PolygonCoordinates.holes() = if (this.size > 1) this.slice(1 until this.size) else listOf()

        /**
         * True if the rings are in the correct order (right hand rule), all rings have the same start and end, and all rings have at least 3 points.
         */
        fun PolygonCoordinates.isValid() = this[0].isCounterClockWise() && this.holes().map { it.isCounterClockWise() }
            .none { it } && this.all { it.hasSameStartAndEnd() } && this.all { it.size > 2 }

        /**
         * True if all polygons in the multipolygon are valid.
         */
        fun MultiPolygonCoordinates.isValid() = this.all { it.isValid() }

        /**
         * Returns a copy of the ring with the points in reverse order.
         */
        fun LinearRingCoordinates.changeOrder() = this.let {
            // make a copy because reverse modifies the array in place.
            val copy = it.copyOf()
            copy.reverse()
            copy
        }

        /**
         * True if the order of the coordinates in the ring is counter clockwise. You can use this to check
         * for the [right hand rule]( https://tools.ietf.org/html/rfc7946#section-3.1.6) on polygons
         */
        fun LinearRingCoordinates.isCounterClockWise() = !this.isClockWise()

        /**
         * True if the order of the coordinates in the ring is clockwise.
         *
         * Based on some suggestions on [stackoverflow](https://stackoverflow.com/a/1165943/1041442)
         */
        fun LinearRingCoordinates.isClockWise(): Boolean {
            // Sum over the edges, (x2 − x1)(y2 + y1): https://stackoverflow.com/a/1165943/1041442
            if (this.size < 2) {
                return true
            }
            var last = this[0]
            var sum = 0.0
            this.slice(1 until this.size).forEach { current ->
                sum += (current.longitude - last.longitude) * (current.latitude + last.latitude)
                last = current
            }
            return sum > 0
        }

        val latPattern = """(?:[+-])?(?:90(?:(?:\.0{1,6})?)|(?:[1-8][0-9]|[0-9])(?:(?:\.[0-9]{1,6})?))"""
        val lonPattern = """(?:\+|-)?(?:180(?:(?:\.0{1,6})?)|(?:1[0-7][0-9]|[1-9][0-9]|[0-9])(?:(?:\.[0-9]{1,6})?))"""
        val latLonRegex = """(?<![0-9.])\s*($latPattern)\s*,\s*($lonPattern)\s*(?![0-9.])""".toRegex()

        fun isValidCoordinate(input: String): Boolean {
            return latLonRegex.matches(input)
        }

        fun parseCoordinate(input: String): PointCoordinates? {
            val match = latLonRegex.matchEntire(input) ?: return null

            val latitude = match.groupValues[1].toDouble()
            val longitude = match.groupValues[2].toDouble()
            return doubleArrayOf(longitude, latitude)
        }

        fun findAllCoordinates(input: String): List<PointCoordinates> {
            return latLonRegex.findAll(input)
                .mapNotNull { match ->
                    val lat = match.groupValues[1].toDoubleOrNull()
                    val lon = match.groupValues[2].toDoubleOrNull()
                    if (lat != null && lon != null) doubleArrayOf(lon, lat) else null
                }
                .toList()
        }
    }
}

