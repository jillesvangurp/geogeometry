/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Note. This class was adapted from lucene's GeoHashUtils; keeping this class licensed under the Apache License for this reason
 */
package com.jillesvangurp.geo

import com.jillesvangurp.geo.GeoGeometry.Companion.overlap
import com.jillesvangurp.geo.GeoGeometry.Companion.polygonContains
import com.jillesvangurp.geojson.*
import kotlin.math.min

private val BITS = intArrayOf(16, 8, 4, 2, 1)

// note: no a,i,l, and o
private val BASE32_CHARS = charArrayOf(
    '0',
    '1',
    '2',
    '3',
    '4',
    '5',
    '6',
    '7',
    '8',
    '9',
    'b',
    'c',
    'd',
    'e',
    'f',
    'g',
    'h',
    'j',
    'k',
    'm',
    'n',
    'p',
    'q',
    'r',
    's',
    't',
    'u',
    'v',
    'w',
    'x',
    'y',
    'z'
)

private fun calculateB32DecodeMap(): Map<Char, Int> {
    val map = mutableMapOf<Char, Int>()
    for (i in BASE32_CHARS.indices) {
        map[BASE32_CHARS[i]] = i
    }
    return map
}

private val BASE32_DECODE_MAP = calculateB32DecodeMap()

/**
 * This class was originally adapted from Apache Lucene's GeoHashUtils.java back in 2012. Please note that this class retains the
 * original licensing (as required), which is different from other classes contained in this project, which are MIT
 * licensed.
 *
 * Relative to the Apache implementation, the code has been cleaned up, expanded, migrated to Kotlin and modified beyond recognition.
 *
 * New methods have been added to facilitate creating sets of geo hashes for e.g. polygons and other geometric forms.
 */
@Suppress("unused")
class GeoHashUtils {
    companion object {
        const val DEFAULT_GEO_HASH_LENGTH = 12

        /**
         * Same as encode but returns a substring of the specified length.
         *
         * @param latitude latitude
         * @param longitude longitude
         * @param length length in characters (1 to 12)
         * @return geo hash of the specified length. The minimum length is 1 and the maximum length is 12.
         */

        fun encode(latitude: Double, longitude: Double, length: Int = DEFAULT_GEO_HASH_LENGTH): String {
            if (length < 1 || length > 12) {
                throw IllegalArgumentException("length must be between 1 and 12")
            }
            GeoGeometry.validate(latitude, longitude, false)
            val latInterval = doubleArrayOf(-90.0, 90.0)
            val lonInterval = doubleArrayOf(-180.0, 180.0)

            val geohash = StringBuilder()
            var isEven = true
            var bit = 0
            var ch = 0

            while (geohash.length < length) {
                if (isEven) {
                    val mid = (lonInterval[0] + lonInterval[1]) / 2
                    if (longitude > mid) {
                        ch = ch or BITS[bit]
                        lonInterval[0] = mid
                    } else {
                        lonInterval[1] = mid
                    }
                } else {
                    val mid: Double = (latInterval[0] + latInterval[1]) / 2
                    if (latitude > mid) {
                        ch = ch or BITS[bit]
                        latInterval[0] = mid
                    } else {
                        latInterval[1] = mid
                    }
                }

                isEven = !isEven

                if (bit < 4) {
                    bit++
                } else {
                    geohash.append(BASE32_CHARS[ch])
                    bit = 0
                    ch = 0
                }
            }
            return geohash.toString()
        }

        /**
         * Encode a geojson style point of [longitude,latitude]
         * @param pointCoordinates point
         * @return geohash
         */

        fun encode(pointCoordinates: PointCoordinates): String {
            return encode(pointCoordinates.latitude, pointCoordinates.longitude, DEFAULT_GEO_HASH_LENGTH)
        }

        /**
         * @param geoHash valid geoHash
         * @return double array representing the bounding box for the geoHash of [south latitude, north latitude, west
         * longitude, east longitude]
         */

        fun decodeBbox(geoHash: String): DoubleArray {
            var south = -90.0
            var north = 90.0
            var west = -180.0
            var east = 180.0

            var isEven = true
            for (i in 0 until geoHash.length) {

                val currentCharacter =
                    BASE32_DECODE_MAP[geoHash[i]] ?: throw IllegalArgumentException("not a base32 character")

                for (mask in BITS) {
                    if (isEven) {
                        if (currentCharacter and mask != 0) {
                            west = (west + east) / 2
                        } else {
                            east = (west + east) / 2
                        }
                    } else {

                        if (currentCharacter and mask != 0) {
                            south = (south + north) / 2
                        } else {
                            north = (south + north) / 2
                        }
                    }
                    isEven = !isEven
                }
            }

            return doubleArrayOf(west, south, east, north)
            // return doubleArrayOf(south, north, west, east)
        }

        /**
         * This decodes the geo hash into it's center. Note that the coordinate that you used to generate the geo hash may
         * be anywhere in the geo hash's bounding box and therefore you should not expect them to be identical.
         *
         * The original apache code attempted to round the returned coordinate. I have chosen to remove this 'feature' since
         * it is useful to know the center of the geo hash as exactly as possible, even for very short geo hashes.
         *
         * Should you wish to apply some rounding, you can use the GeoGeometry.roundToDecimals method.
         *
         * @param geoHash valid geo hash
         * @return a coordinate representing the center of the geohash as a double array of [longitude,latitude]
         */

        fun decode(geoHash: String): PointCoordinates {
            val bbox = decodeBbox(geoHash)

            val latitude = (bbox.southLatitude + bbox.northLatitude) / 2
            val longitude = (bbox.eastLongitude + bbox.westLongitude) / 2

            return doubleArrayOf(longitude, latitude)
        }

        /**
         * @param geoHash geohash
         * @return the geo hash of the same length directly south of the bounding box.
         */

        fun south(geoHash: String): String {
            val bbox = decodeBbox(geoHash)
            val latDiff = bbox.northLatitude - bbox.southLatitude
            val lat = bbox.southLatitude - latDiff / 2
            val lon = (bbox.eastLongitude + bbox.westLongitude) / 2
            return encode(lat, lon, geoHash.length)
        }

        /**
         * @param geoHash geohash
         * @return the geo hash of the same length directly north of the bounding box.
         */

        fun north(geoHash: String): String {
            val bbox = decodeBbox(geoHash)
            val latDiff = bbox.northLatitude - bbox.southLatitude
            val lat = bbox.northLatitude + latDiff / 2
            val lon = (bbox.eastLongitude + bbox.westLongitude) / 2
            return encode(lat, lon, geoHash.length)
        }

        /**
         * @param geoHash geohash
         * @return the geo hash of the same length directly west of the bounding box.
         */

        fun west(geoHash: String): String {
            val bbox = decodeBbox(geoHash)
            val lonDiff = bbox.eastLongitude - bbox.westLongitude
            val lat = (bbox.southLatitude + bbox.northLatitude) / 2
            var lon = bbox.westLongitude - lonDiff / 2
            if (lon < -180) {
                lon = 180 - (lon + 180)
            }
            if (lon > 180) {
                lon = 180.0
            }

            return encode(lat, lon, geoHash.length)
        }

        /**
         * @param geoHash geohash
         * @return the geo hash of the same length directly east of the bounding box.
         */

        fun east(geoHash: String): String {
            val bbox = decodeBbox(geoHash)
            val lonDiff = bbox.eastLongitude - bbox.westLongitude
            val lat = (bbox.southLatitude + bbox.northLatitude) / 2
            var lon = bbox.eastLongitude + lonDiff / 2

            if (lon > 180) {
                lon = -180 + (lon - 180)
            }
            if (lon < -180) {
                lon = -180.0
            }

            return encode(lat, lon, geoHash.length)
        }

        /**
         * @param geoHash geo hash
         * @param latitude latitude
         * @param longitude longitude
         * @return true if the coordinate is contained by the bounding box for this geo hash
         */

        fun contains(geoHash: String, latitude: Double, longitude: Double): Boolean {
            return GeoGeometry.bboxContains(decodeBbox(geoHash), latitude, longitude)
        }

        /**
         * Return the 32 geo hashes this geohash can be divided into.
         *
         * They are returned alpabetically sorted but in the real world they follow this pattern:
         *
         * <pre>
         * u33dbfc0 u33dbfc2 | u33dbfc8 u33dbfcb
         * u33dbfc1 u33dbfc3 | u33dbfc9 u33dbfcc
         * -------------------------------------
         * u33dbfc4 u33dbfc6 | u33dbfcd u33dbfcf
         * u33dbfc5 u33dbfc7 | u33dbfce u33dbfcg
         * -------------------------------------
         * u33dbfch u33dbfck | u33dbfcs u33dbfcu
         * u33dbfcj u33dbfcm | u33dbfct u33dbfcv
         * -------------------------------------
         * u33dbfcn u33dbfcq | u33dbfcw u33dbfcy
         * u33dbfcp u33dbfcr | u33dbfcx u33dbfcz
         </pre> *
         *
         * the first 4 share the north east 1/8th the first 8 share the north east 1/4th the first 16 share the north 1/2
         * and so on.
         *
         * They are ordered as follows:
         *
         * <pre>
         * 0  2  8 10
         * 1  3  9 11
         * 4  6 12 14
         * 5  7 13 15
         * 16 18 24 26
         * 17 19 25 27
         * 20 22 28 30
         * 21 23 29 31
         </pre> *
         *
         * Some useful properties: Anything ending with
         *
         * <pre>
         * 0-g = N
         * h-z = S
         *
         * 0-7 = NW
         * 8-g = NE
         * h-r = SW
         * s-z = SE
         </pre> *
         *
         * @param geoHash geo hash
         * @return String array with the geo hashes.
         */

        fun subHashes(geoHash: String): Array<String> {
            val list = mutableListOf<String>()
            for (c in BASE32_CHARS) {
                list.add(geoHash + c)
            }
            return list.toTypedArray()
        }

        /**
         * @param geoHash geo hash
         * @return the 16 northern sub hashes of the geo hash
         */

        fun subHashesNorth(geoHash: String): Array<String> {
            val list = mutableListOf<String>()
            for (c in BASE32_CHARS) {
                if (c <= 'g') {
                    list.add(geoHash + c)
                }
            }
            return list.toTypedArray()
        }

        /**
         * @param geoHash geo hash
         * @return the 16 southern sub hashes of the geo hash
         */

        fun subHashesSouth(geoHash: String): Array<String> {
            val list = mutableListOf<String>()
            for (c in BASE32_CHARS) {
                if (c >= 'h') {
                    list.add(geoHash + c)
                }
            }
            return list.toTypedArray()
        }

        /**
         * @param geoHash geo hash
         * @return the 8 north-west sub hashes of the geo hash
         */

        fun subHashesNorthWest(geoHash: String): Array<String> {
            val list = mutableListOf<String>()
            for (c in BASE32_CHARS) {
                if (c <= '7') {
                    list.add(geoHash + c)
                }
            }
            return list.toTypedArray()
        }

        /**
         * @param geoHash geo hash
         * @return the 8 north-east sub hashes of the geo hash
         */

        fun subHashesNorthEast(geoHash: String): Array<String> {
            val list = mutableListOf<String>()
            for (c in BASE32_CHARS) {
                if (c in '8'..'g') {
                    list.add(geoHash + c)
                }
            }
            return list.toTypedArray()
        }

        /**
         * @param geoHash geo hash
         * @return the 8 south-west sub hashes of the geo hash
         */

        fun subHashesSouthWest(geoHash: String): Array<String> {
            val list = mutableListOf<String>()
            for (c in BASE32_CHARS) {
                if (c in 'h'..'r') {
                    list.add(geoHash + c)
                }
            }
            return list.toTypedArray()
        }

        /**
         * @param geoHash geo hash
         * @return the 8 south-east sub hashes of the geo hash
         */

        fun subHashesSouthEast(geoHash: String): Array<String> {
            val list = mutableListOf<String>()
            for (c in BASE32_CHARS) {
                if (c >= 's') {
                    list.add(geoHash + c)
                }
            }
            return list.toTypedArray()
        }

        fun geoHashesForPolygon(
            coordinates: PolygonCoordinates,
            maxLength: Int? = null,
            includePartial: Boolean = false
        ): Set<String> {
            // TODO implement removal of hole hashes?
            return geoHashesForLinearRing(
                coordinates = coordinates[0],
                maxLength = maxLength,
                includePartial = includePartial
            )
        }

        fun geoHashesForMultiPolygon(
            coordinates: MultiPolygonCoordinates,
            maxLength: Int? = null,
            includePartial: Boolean = false
        ): Set<String> {
            return coordinates.flatMap {
                geoHashesForPolygon(
                    coordinates = it,
                    maxLength = maxLength,
                    includePartial = includePartial
                )
            }.toSet()
        }

        /**
         * Cover the polygon with geo hashes. Call  with a
         * maxLength that is the suitable hashlength for the surrounding bounding box + 1. If you need more fine grained
         * boxes, specify your own maxLength.
         *
         * Note, the algorithm 'fills' the polygon from the inside with hashes. So, if a geohash partially falls outside the
         * polygon, it is omitted. So, if you have a polygon with a lot of detail, this may result in large portions not
         * being covered. To resolve this, manually choose a bigger geohash length. This results, in more but smaller
         * geohashes around the edges.
         *
         * The algorithm works for both convex and concave algorithms. Important: for concave polygons, be sure to
         * pick a hash size small enough to cover the narrowest part of the polygon. Otherwise, you may parts of the
         * polygon that are not covered.
         *
         * @param maxLength
         * maximum length of the geoHash; the more you specify, the more expensive it gets
         * @param coordinates
         * @param includePartial when true it includes hashes on the border of he polygon. When false, it fills from the inside without including the partial hashes.
         * 2d array of polygonPoints points that make up the polygon as arrays of [longitude, latitude]
         * @return a set of geo hashes that cover the polygon area.
         */
        fun geoHashesForLinearRing(
            maxLength: Int? = null,
            coordinates: LinearRingCoordinates,
            includePartial: Boolean = false
        ): Set<String> {
            for (point in coordinates) {
                // basically the algorithm can go into an endless loop. Best to avoid the poles.
                if (point.latitude < -89.5 || point.latitude > 89.5) {
                    throw IllegalArgumentException(
                        "please stay away from the north pole or the south pole; there are some known issues there. Besides, nothing there but snow and ice."
                    )
                }
            }

            maxLength?.let {
                if (maxLength < 1 || maxLength >= DEFAULT_GEO_HASH_LENGTH) {
                    throw IllegalArgumentException("maxLength should be between 2 and $DEFAULT_GEO_HASH_LENGTH was $maxLength")
                }
            }
            val bbox = GeoGeometry.boundingBox(coordinates)
            // first lets figure out an appropriate geohash length

            // if shit breaks, this is useful for wtf'ing in geojson.io
            // val f = arrayOf(outerPolygonLinearRing) as PolygonCoordinates
            // println(f.stringify())
            // println(bbox.polygon().coordinates?.map { "[${it.map { p -> "[${p[0]},${p[1]}]" }.joinToString(", ")}]"  }?.joinToString(", "))

            // first lets figure out an appropriate geohash length
            val southLat = bbox.southLatitude
            val northLat = bbox.northLatitude
            val westLon = bbox.westLongitude
            val eastLon = bbox.eastLongitude

            val diagonal = GeoGeometry.distance(southLat, westLon, northLat, eastLon)
            val hashLength = min(
                maxLength ?: 12,
                suitableHashLength(diagonal, southLat, westLon) + 1
            ) // (+1 means small boxes on the border)

            var partiallyContained: MutableSet<String> = HashSet()
            // now lets generate all geohashes for the containing bounding box
            // lets start at the top left:

            var rowHash = encode(southLat, westLon, hashLength)

            // we start by covering the entire bbox with hashes of the same length
            // we do this by scanning west to east and south to north
            var rowBox = decodeBbox(rowHash)
            while (rowBox.southLatitude < northLat) {
                var columnHash = rowHash
                var columnBox = rowBox

                while (isWest(columnBox.westLongitude, eastLon)) {

                    partiallyContained.add(columnHash)
                    // move column east
                    columnHash = east(columnHash)
                    columnBox = decodeBbox(columnHash)
                }

                // move row north
                rowHash = north(rowHash)
                rowBox = decodeBbox(rowHash)
            }

            // useful for debugging so keep this for now
            // val features = FeatureCollection.of(bbox.polygon().asFeature(), PolygonGeometry(arrayOf(outerPolygonLinearRing as LinearRingCoordinates)).asFeature())
            // println(gson.toJson(FeatureCollection.fromGeoHashes(partiallyContained) + features))

            val fullyContained = mutableSetOf<String>()

            var detail = hashLength

            // next we 'refine' our hashes by tossing out the ones outside the polygon
            // and splitting the remaining ones in their 32 sub hashes if they are not full contained
            // we're not aiming for perfect detail here in terms of 'pixellation', 6
            // extra chars in the geohash ought to be enough and going beyond 9
            // doesn't serve much purpose.
            // we need to sometimes go beyond maxLength if we found no fully contained hashes.
            // WARNING this can get ugly in terms of numbers of hashes
            while (detail < (maxLength ?: hashLength) || fullyContained.size == 0) {
                partiallyContained =
                    splitAndFilter(coordinates, fullyContained, partiallyContained)
                detail++
            }
            // fallback
            if (fullyContained.size == 0) {
                fullyContained.addAll(partiallyContained)
            }
            if (includePartial) {
                partiallyContained.forEach { hash ->
                    decodeBbox(hash).polygon().coordinates?.get(0)?.let { ring ->
                        if (ring.firstOrNull { polygonContains(it, arrayOf(coordinates)) } != null) {
                            fullyContained.add(hash)
                        }
                    }
                }
            }
            return if (maxLength != null && detail> maxLength) {
                // replace tiny hashes with bigger ones
                val filtered = mutableSetOf<String>()
                fullyContained.forEach { if (it.length <= hashLength) filtered.add(it) else filtered.add(it.subSequence(0, maxLength).toString()) }
                filtered
            } else {
                fullyContained
            }
        }

        private fun splitAndFilter(
            coordinates: LinearRingCoordinates,
            fullyContained: MutableSet<String>,
            partiallyContained: Set<String>
        ): MutableSet<String> {
            val stillPartial = HashSet<String>()
            val checkCompleteArea = HashSet<String>(32, 1.0f)
            // now we need to break up the partially contained hashes
            for (hash in partiallyContained) {
                checkCompleteArea.clear()
                for (h in subHashes(hash)) {
                    val hashBbox = decodeBbox(h)

                    val containsNw = polygonContains(hashBbox.northWest, coordinates)
                    val containsNe = polygonContains(hashBbox.northEast, coordinates)
                    val containsSw = polygonContains(hashBbox.southWest, coordinates)
                    val containsSe = polygonContains(hashBbox.southEast, coordinates)

                    when {
                        containsNw && containsNe && containsSw && containsSe -> {
                            // all four corners are inside the polygon
                            checkCompleteArea.add(h)
                        }
                        overlap(hashBbox.polygon().coordinates!!, arrayOf(coordinates)) -> {
                            stillPartial.add(h)
                        }
                    }
                }
                if (checkCompleteArea.size == BASE32_CHARS.size) {
                    fullyContained.add(hash)
                } else {
                    fullyContained.addAll(checkCompleteArea)
                }
            }

            return stillPartial
        }

        /**
         * @param hashLength desired length of the geohash
         * @param wayPoints line string
         * @return set of geo hashes along the path with the specified geo hash length
         */
        fun geoHashesForPath(hashLength: Int, wayPoints: LineStringCoordinates): Set<String> {
            if (wayPoints.size < 2) {
                throw IllegalArgumentException("must have at least two way points on the path")
            }
            val hashes = mutableSetOf<String>()
            // The slope of the line through points A(ax, ay) and B(bx, by) is given
            // by m = (by-ay)/(bx-ax) and the equation of this
            // line can be written y = m(x - ax) + ay.

            for (i in 1 until wayPoints.size) {
                val previousPoint = wayPoints[i - 1]
                val point = wayPoints[i]
                hashes.addAll(
                    geoHashesForLine(
                        hashLength.toDouble(),
                        previousPoint.latitude,
                        previousPoint.longitude,
                        point.latitude,
                        point.longitude
                    )
                )
            }

            return hashes
        }

        /**
         * @param width in meters
         * @param lat1 latitude
         * @param lon1 longitude
         * @param lat2 latitude
         * @param lon2 longitude
         * @return set of geo hashes along the line with the specified geo hash length.
         */

        fun geoHashesForLine(
            width: Double,
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
            maxLength: Int? = null
        ): Set<String> {
            if (lat1 == lat2 && lon1 == lon2) {
                return setOf(encode(lat1, lon1))
            }

            val hashLength = maxLength ?: suitableHashLength(width, lat1, lon1)
            val (h1, b1) = encodeWithBbox(lat1, lon1, hashLength)
            val (h2, b2) = encodeWithBbox(lat2, lon2, hashLength)

            return if (h1 == h2) {
                // same geohash for begin and end, effectively a point
                setOf(h1)
            } else {
                geoHashesForLinearRing(
                    maxLength = hashLength,
                    // linear ring around the line
                    coordinates = arrayOf(
                        doubleArrayOf(b1.westLongitude, b1.southLatitude),
                        doubleArrayOf(b1.eastLongitude, b1.southLatitude),
                        doubleArrayOf(b2.eastLongitude, b2.northLatitude),
                        doubleArrayOf(b2.westLongitude, b2.northLatitude),
                        doubleArrayOf(b1.westLongitude, b1.southLatitude)
                    )
                )
            }
        }

        /**
         * @param l1 longitude
         * @param l2 longitude
         * @return true if l1 is west of l2
         */

        fun isWest(l1: Double, l2: Double): Boolean {
            val ll1 = l1 + 180
            val ll2 = l2 + 180
            return if (ll1 < ll2 && ll2 - ll1 < 180) {
                true
            } else
                ll1 > ll2 && ll2 + 360 - ll1 < 180
        }

        /**
         * @param l1 longitude
         * @param l2 longitude
         * @return true if l1 is east of l2
         */

        fun isEast(l1: Double, l2: Double): Boolean {
            val ll1 = l1 + 180
            val ll2 = l2 + 180
            return if (ll1 > ll2 && ll1 - ll2 < 180) {
                true
            } else
                ll1 < ll2 && ll1 + 360 - ll2 < 180
        }

        /**
         * @param l1 latitude
         * @param l2 latitude
         * @return true if l1 is north of l2
         */

        fun isNorth(l1: Double, l2: Double): Boolean {
            return l1 > l2
        }

        /**
         * @param l1 latitude
         * @param l2 latitude
         * @return true if l1 is south of l2
         */

        fun isSouth(l1: Double, l2: Double): Boolean {
            return l1 < l2
        }

        private fun encodeWithBbox(latitude: Double, longitude: Double, length: Int): Pair<String, BoundingBox> {
            if (length < 1 || length > 12) {
                throw IllegalArgumentException("length must be between 1 and 12")
            }
            val latInterval = doubleArrayOf(-90.0, 90.0)
            val lonInterval = doubleArrayOf(-180.0, 180.0)

            val geohash = StringBuilder()
            var isEven = true
            var bit = 0
            var ch = 0

            while (geohash.length < length) {
                val mid: Double
                if (isEven) {
                    mid = (lonInterval[0] + lonInterval[1]) / 2
                    if (longitude > mid) {
                        ch = ch or BITS[bit]
                        lonInterval[0] = mid
                    } else {
                        lonInterval[1] = mid
                    }
                } else {
                    mid = (latInterval[0] + latInterval[1]) / 2
                    if (latitude > mid) {
                        ch = ch or BITS[bit]
                        latInterval[0] = mid
                    } else {
                        latInterval[1] = mid
                    }
                }

                isEven = !isEven

                if (bit < 4) {
                    bit++
                } else {
                    geohash.append(BASE32_CHARS[ch])
                    bit = 0
                    ch = 0
                }
            }
            val southLat = latInterval[0]
            val northLat = latInterval[1]
            val westLon = lonInterval[0]
            val eastLon = lonInterval[1]
            return geohash.toString() to doubleArrayOf(westLon, southLat, eastLon, northLat)
        }

        /**
         * @param maxLength geohash length
         * @param latitude latitude
         * @param longitude longitude
         * @param radius radius in meters
         * @param includePartial include hashes on the circle polygon border as well; defaults to false
         * @return set of geohashes
         */

        fun geoHashesForCircle(
            maxLength: Int?,
            latitude: Double,
            longitude: Double,
            radius: Double,
            includePartial: Boolean = false,
            segments: Int = 20
        ): Set<String> {
            val circle2polygon = GeoGeometry.circle2polygon(segments, latitude, longitude, radius)
            return geoHashesForLinearRing(
                maxLength = maxLength,
                coordinates = circle2polygon[0],
                includePartial = includePartial
            )
        }

        /**
         * @param granularityInMeters granularity
         * @param latitude latitude
         * @param longitude longitude
         * @return the largest hash length where the hash bbox has a width less than granularityInMeters.
         */
        fun suitableHashLength(granularityInMeters: Double, latitude: Double, longitude: Double): Int {
            var hash = encode(latitude, longitude)
            var width = 0.0
            var length = hash.length
            // the height is the same at for any latitude given a length, but the width converges towards the poles
            while (width < granularityInMeters && hash.isNotEmpty()) {
                length = hash.length
                val bbox = decodeBbox(hash)
                width =
                    GeoGeometry.distance(bbox.westLongitude, bbox.northLatitude, bbox.eastLongitude, bbox.northLatitude)
                hash = hash.substring(0, hash.length - 1)
            }

            return min(length + 1, DEFAULT_GEO_HASH_LENGTH)
        }
    }
}
