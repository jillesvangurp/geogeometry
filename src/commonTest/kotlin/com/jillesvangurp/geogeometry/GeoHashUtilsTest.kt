package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geojson.*
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.min
import kotlin.test.Test

val sampleHashesAndCoordinates = listOf(
    doubleArrayOf(-0.1, 0.1) to "ebpbtdpntc6e",
    doubleArrayOf(13.394904, 52.530888) to "u33dbfcyegk2"
)

class GeoHashUtilsTest {
    val coordinatesWithHashes = arrayOf(
        row(0.1, -0.1, "ebpbtdpntc6e"),
        row(52.530888, 13.394904, "u33dbfcyegk2")
    )

    @Test
    fun shouldDecodeHashes() {
        forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
            val decoded = GeoHashUtils.decode(geoHash)
            decoded.latitude shouldBeApproximately lat
            decoded.longitude shouldBeApproximately lon
        }
    }

    @Test
    fun shouldEncodeHashes() {
        forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
            GeoHashUtils.encode(lat, lon) shouldBe geoHash
            GeoHashUtils.encode(doubleArrayOf(lon, lat)) shouldBe geoHash
        }
    }

    @Test
    fun shouldContainCoordinateInBbox() {
        forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
            GeoHashUtils.contains(geoHash, lat, lon) shouldBe true
            GeoHashUtils.contains(geoHash, lon, lat) shouldBe false
        }
    }

    @Test
    fun shouldGenerateFewerHashesWhenAsked() {
        val hash = GeoHashUtils.encode(52.0, 13.0).subSequence(0, 5)
        val bbox = GeoHashUtils.decodeBbox(hash.toString())
        val p = bbox.polygon()
        val hashes = GeoHashUtils.geoHashesForPolygon(p.coordinates!!.toTypedArray(), maxLength = 5, includePartial = true)
//        println(hashes.joinToString(","))
        hashes.size shouldBeLessThan 5
    }

    @Test
    fun shouldCoverConcavePolygon() {
        val concavePolygon = """
      {
        "type": "Polygon",
        "coordinates": [
          [
            [
              13.402633666992188,
              52.556785714011625
            ],
            [
              13.402719497680664,
              52.54713081557263
            ],
            [
              13.41379165649414,
              52.547078621160054
            ],
            [
              13.413705825805664,
              52.54968826575346
            ],
            [
              13.405895233154297,
              52.54927073304618
            ],
            [
              13.40580940246582,
              52.55459397751005
            ],
            [
              13.413963317871094,
              52.55433304920524
            ],
            [
              13.413877487182617,
              52.55683789687965
            ],
            [
              13.402633666992188,
              52.556785714011625
            ]
          ]
        ]
      }            
        """.trimIndent()
        val p = json.decodeFromString(Geometry.serializer(), concavePolygon) as Geometry.Polygon
        val coordinates = p.coordinates?.asArray?.get(0) ?: throw IllegalStateException()
        val hashes = GeoHashUtils.geoHashesForLinearRing(coordinates = coordinates, includePartial = true)

        println(hashes.size)
        println(json.encodeToString(FeatureCollection.serializer(), FeatureCollection.fromGeoHashes(hashes)))

        val totalHashedArea = hashes.map { GeoHashUtils.decodeBbox(it) }.sumOf { GeoGeometry.area(it) }
        val bboxArea = GeoGeometry.area(GeoGeometry.boundingBox(p.coordinates?.asArray as PolygonCoordinates))
        // it's a concave polygon so the area of the hashes should be smaller than that of the
        // bounding box containing the polygon
        totalHashedArea shouldBeLessThan bboxArea * 0.7
    }

    @Test
    fun shouldDecode() {
        io.kotest.assertions.assertSoftly {
            sampleHashesAndCoordinates.forEach { (point, geoHash) ->

                val decoded = GeoHashUtils.decode(geoHash)

                assertSimilar(point.latitude, decoded[1])
                assertSimilar(point.longitude, decoded[0])
            }
        }
    }

    @Test
    fun shouldEncode() {
        sampleHashesAndCoordinates.forEach { (point, geoHash) ->

            GeoHashUtils.encode(
                point.latitude,
                point.longitude,
                GeoHashUtils.DEFAULT_GEO_HASH_LENGTH
            ) shouldBe geoHash
            GeoHashUtils.encode(point) shouldBe geoHash
        }
    }

    @Test
    fun shouldContainCoordinate() {
        sampleHashesAndCoordinates.forEach { (point, geoHash) ->
            // hash should contain the coordinate
            GeoHashUtils.contains(geoHash, point.latitude, point.longitude) shouldBe true
            // hash should not contain the swapped coordinate
            GeoHashUtils.contains(geoHash, point.longitude, point.latitude) shouldBe false
        }
    }

    @Test
    fun shouldDecodeBbox() {
        sampleHashesAndCoordinates.forEach { (point, geoHash) ->
            val bbox = GeoHashUtils.decodeBbox(geoHash)
            abs((bbox[0] + bbox[2]) / 2 - point.longitude) shouldBeApproximately 0.0
            abs((bbox[1] + bbox[3]) / 2 - point.latitude) shouldBeApproximately 0.0
        }
    }

    @Test
    fun shouldDecodeBbox2() {
        forAll(
            row(0.1, -0.1, "ebpbtdpntc6e"), row(52.530888, 13.394904, "u33dbfcyegk2")
        ) { lat: Double, lon: Double, geoHash: String ->
            val bbox = GeoHashUtils.decodeBbox(geoHash)

            abs((bbox.southLatitude + bbox.northLatitude) / 2 - lat) shouldBeApproximately 0.0
            abs((bbox.westLongitude + bbox.eastLongitude) / 2 - lon) shouldBeApproximately 0.0
        }
    }

    @Test
    fun shouldCalculateEast() {
        sampleHashesAndCoordinates.forEach { (point, geoHash) ->

            val original = geoHash.substring(0, 3)
            val calculated = GeoHashUtils.east(geoHash.substring(0, 3))
            // east hash should not contain the coordinate
            GeoHashUtils.contains(calculated, point.latitude, point.longitude) shouldBe false
            val bbox = GeoHashUtils.decodeBbox(original)
            val eastBbox = GeoHashUtils.decodeBbox(calculated)
            assertSimilar(bbox[1], eastBbox[1])
            assertSimilar(bbox[3], eastBbox[3])
            assertSimilar(bbox[2], eastBbox[0])
            assertSimilar((eastBbox[2] - bbox[0]) / 2, bbox[2] - bbox[0])
            val nl = GeoHashUtils.decode(calculated)[0]
            val ol = GeoHashUtils.decode(original)[0]
            // decoded hash lon should be east of original
            GeoHashUtils.isEast(nl, ol) shouldBe true
        }
    }

    @Test
    fun shouldCalculateSouth() {
        sampleHashesAndCoordinates.forEach { (point, geoHash) ->

            val original = geoHash.substring(0, 3)
            val calculatedHash = GeoHashUtils.south(original)
            //        System.out.println(original + " " + calculatedHash);
            val oBox = GeoHashUtils.decodeBbox(original)
            val cBox = GeoHashUtils.decodeBbox(calculatedHash)
            // calculated hash should not contain the coordinate
            GeoHashUtils.contains(calculatedHash, point.latitude, point.longitude) shouldBe false
            val oWest = oBox[0]
            val oSouth = oBox[1]
            val oEast = oBox[2]
            val oNorth = oBox[3]
            val cWest = cBox[0]
            val cSouth = cBox[1]
            val cEast = cBox[2]
            val cNorth = cBox[3]
            assertSimilar((oNorth - cSouth) / 2, oNorth - oSouth)
            assertSimilar(oSouth, cNorth)
            assertSimilar(oEast, cEast)
            assertSimilar(oWest, cWest)
            val nl = GeoHashUtils.decode(calculatedHash)[1]
            val ol = GeoHashUtils.decode(original)[1]
            // decoded hash lat should be south of original
            GeoHashUtils.isSouth(nl, ol) shouldBe true
        }
    }

    @Test
    fun shouldCalculateNorth() {
        sampleHashesAndCoordinates.forEach { (point, geoHash) ->
            val original = geoHash.substring(0, 3)
            val calculatedHash = GeoHashUtils.north(original)
            val oBox = GeoHashUtils.decodeBbox(original)
            val cBox = GeoHashUtils.decodeBbox(calculatedHash)
            // calculated hash should not contain the coordinate
            GeoHashUtils.contains(calculatedHash, point.latitude, point.longitude) shouldBe false
            val oWest = oBox[0]
            val oSouth = oBox[1]
            val oEast = oBox[2]
            val oNorth = oBox[3]
            val cWest = cBox[0]
            val cSouth = cBox[1]
            val cEast = cBox[2]
            val cNorth = cBox[3]
            assertSimilar(oNorth, cSouth)
            assertSimilar((oSouth - cNorth) / 2, oSouth - oNorth)
            assertSimilar(oWest, cWest)
            assertSimilar(oEast, cEast)
            //        System.out.println();
            val nl = GeoHashUtils.decode(calculatedHash)[1]
            val ol = GeoHashUtils.decode(original)[1]
            // decoded hash lat should be north of original
            GeoHashUtils.isNorth(nl, ol) shouldBe true
        }
    }

    @Test
    fun shouldCalculateWest() {
        sampleHashesAndCoordinates.forEach { (point, geoHash) ->

            val original = geoHash.substring(0, 3)
            val calculatedHash = GeoHashUtils.west(original)
            val oBox = GeoHashUtils.decodeBbox(original)
            val cBox = GeoHashUtils.decodeBbox(calculatedHash)
            // calculated hash should not contain the coordinate
            GeoHashUtils.contains(calculatedHash, point.latitude, point.longitude) shouldBe false
            val oWest = oBox[0]
            val oSouth = oBox[1]
            val oEast = oBox[2]
            val oNorth = oBox[3]
            val cWest = cBox[0]
            val cSouth = cBox[1]
            val cEast = cBox[2]
            val cNorth = cBox[3]
            assertSimilar(oSouth, cSouth)
            assertSimilar(oNorth, cNorth)
            assertSimilar((oEast - cWest) / 2, oEast - oWest)
            assertSimilar(oWest, cEast)
            val nl = GeoHashUtils.decode(calculatedHash)[0]
            val ol = GeoHashUtils.decode(original)[0]
            // decoded hash lon should be west of original
            GeoHashUtils.isWest(nl, ol) shouldBe true
        }
    }

    @Test
    fun shouldCalculateHashesForLine() {
        arrayOf(
            arrayOf(1, 1, 2, 2),
            arrayOf(2, 2, 1, 1),
            arrayOf(2, 1, 1, 1),
            arrayOf(1, 2, 1, 1),
            arrayOf(1, 1, 2, 1),
            arrayOf(1, 1, 1, 2),
            arrayOf(1, 1, 1, 2)
        ).forEach { (lat1, lon1, lat2, lon2) ->

            val hashes = GeoHashUtils.geoHashesForLine(
                width = 1000.0, lat1 = lat1.toDouble(), lon1 = lon1.toDouble(), lat2 = lat2.toDouble(),
                lon2 = lon2.toDouble(), maxLength = 5
            )
            hashes shouldHaveAtLeastSize 10
        }
    }

    @Test
    fun shouldCalculateHashLength() {
        arrayOf(
            arrayOf(10.0, 85.0, 15.0),
            arrayOf(10.0, 50.0, 15.0),
            arrayOf(10.0, 0.0, 15.0),
            arrayOf(100.0, 85.0, 15.0),
            arrayOf(100.0, 50.0, 15.0),
            arrayOf(100.0, 0.0, 15.0),
            arrayOf(1000.0, 85.0, 15.0),
            arrayOf(1000.0, 50.0, 15.0),
            arrayOf(1000.0, 0.0, 15.0),
            arrayOf(10000.0, 85.0, 15.0),
            arrayOf(10000.0, 50.0, 15.0),
            arrayOf(10000.0, 0.0, 15.0),
            arrayOf(100000.0, 85.0, 15.0),
            arrayOf(100000.0, 50.0, 15.0),
            arrayOf(100000.0, 0.0, 15.0)
        ).forEach { (m, latitude, longitude) ->
            val length = GeoHashUtils.suitableHashLength(m, latitude, longitude)
            val hash = GeoHashUtils.encode(latitude, longitude, length)
            val bbox = GeoHashUtils.decodeBbox(hash)
            val distance = GeoGeometry.distance(bbox[0], bbox[1], bbox[0], bbox[3])
            distance shouldBeLessThan m
        }
    }

    @Test
    fun shouldCalculateEastOn180() {
        val hash = GeoHashUtils.encode(-18.0, 179.9, 3)
        var bbox = GeoHashUtils.decodeBbox(hash)
        bbox[2] shouldBe 180.0
        val east = GeoHashUtils.east(hash)
        bbox = GeoHashUtils.decodeBbox(east)
        bbox[0] shouldBe -180.0
    }

    @Test
    fun shouldCalculateWestOn180() {
        val hash = GeoHashUtils.encode(-18.0, -179.9, 3)
        var bbox = GeoHashUtils.decodeBbox(hash)
        bbox[0] shouldBe -180.0
        val west = GeoHashUtils.west(hash)
        bbox = GeoHashUtils.decodeBbox(west)
        bbox[2] shouldBe 180.0
    }

    @Test
    fun shouldCalculateHashesForPolygon() {
        val polygon = arrayOf(
            doubleArrayOf(-1.0, 1.0),
            doubleArrayOf(2.0, 2.0),
            doubleArrayOf(3.0, -1.0),
            doubleArrayOf(-2.0, -4.0)
        )
        var min = 10
        val geoHashesForPolygon = GeoHashUtils.geoHashesForLinearRing(
            maxLength = 5, coordinates = polygon
        )
        for (h in geoHashesForPolygon) {
            min = min(min, h.length)
        }
        // there should be some hashes with length=4
        min shouldBe 4
        // huge area, should generate lots of hashes
        geoHashesForPolygon shouldHaveAtLeastSize 1000
    }

    @Test
    fun shouldCalculateHashesForCircle() {
        val hashesForCircle =
            GeoHashUtils.geoHashesForCircle(6, 52.0, 13.0, 200.0)
        for (hash in hashesForCircle) {
            val point = GeoHashUtils.decode(hash)
            val distance = GeoGeometry.distance(point, doubleArrayOf(13.0, 52.0))
            distance shouldBeLessThan 200.0
        }
    }

    @Test
    fun shouldCheckIfWest() {
        // should be west
        GeoHashUtils.isWest(90.0, 91.0) shouldBe true
        GeoHashUtils.isWest(-1.0, 1.0) shouldBe true
        GeoHashUtils.isWest(-89.0, 90.0) shouldBe true
        GeoHashUtils.isWest(180.0, -178.0) shouldBe true
        GeoHashUtils.isWest(180.0, -179.99527198651967) shouldBe true

        // should not be west
        GeoHashUtils.isWest(-179.0, 180.0) shouldBe false
        GeoHashUtils.isWest(91.0, 90.0) shouldBe false
        GeoHashUtils.isWest(-179.0, 180.0) shouldBe false
        GeoHashUtils.isWest(89.0, -90.0) shouldBe false
        GeoHashUtils.isWest(1.0, -1.0) shouldBe false
        GeoHashUtils.isWest(91.0, 90.0) shouldBe false
        GeoHashUtils.isWest(-91.0, 90.0) shouldBe false
    }

    @Test
    fun shouldCheckIfEast() {
        // should not be east
        GeoHashUtils.isEast(90.0, 91.0) shouldBe false
        GeoHashUtils.isEast(-1.0, 1.0) shouldBe false
        GeoHashUtils.isEast(-89.0, 90.0) shouldBe false
        GeoHashUtils.isEast(180.0, -178.0) shouldBe false
        GeoHashUtils.isEast(180.0, -179.99527198651967) shouldBe false

        // should be east
        GeoHashUtils.isEast(-179.0, 180.0) shouldBe true
        GeoHashUtils.isEast(91.0, 90.0) shouldBe true
        GeoHashUtils.isEast(-179.0, 180.0) shouldBe true
        GeoHashUtils.isEast(89.0, -90.0) shouldBe true
        GeoHashUtils.isEast(1.0, -1.0) shouldBe true
        GeoHashUtils.isEast(91.0, 90.0) shouldBe true
        GeoHashUtils.isEast(-91.0, 90.0) shouldBe true
    }

    @Test
    fun shouldBeNeitherWestNorEast() {
        // should not be west
        GeoHashUtils.isWest(-90.0, 90.0) shouldBe false
        // should not be east
        GeoHashUtils.isEast(-90.0, 90.0) shouldBe false
    }

    @Test
    fun shouldGenerateCircleHashesThatAreAllWithinRadiusOfCircle() {
        val latitude = 52.529731
        val longitude = 13.401284
        val radius = 100
        val hashes = GeoHashUtils.geoHashesForCircle(8, latitude, longitude, radius.toDouble())
        println(json.encodeToString(FeatureCollection.serializer(), FeatureCollection.fromGeoHashes(hashes)))
        println(hashes.size)
        for (hash in hashes) {
            GeoGeometry.distance(
                GeoHashUtils.decode(
                    hash
                ),
                doubleArrayOf(longitude, latitude)
            ) shouldBeLessThan radius.toDouble() * 1.3
        }
    }

    @Test
    fun shouldOverlap() {
        val polygon = arrayOf(
            doubleArrayOf(50.0, 15.0),
            doubleArrayOf(53.0, 15.0),
            doubleArrayOf(53.0, 11.0),
            doubleArrayOf(50.0, 11.0)
        )
        val p2overlapping = arrayOf(
            doubleArrayOf(51.0, 16.0),
            doubleArrayOf(52.0, 16.0),
            doubleArrayOf(52.0, 10.0),
            doubleArrayOf(51.0, 10.0)
        )
        val p3outside = arrayOf(
            doubleArrayOf(60.0, 15.0),
            doubleArrayOf(63.0, 15.0),
            doubleArrayOf(63.0, 11.0),
            doubleArrayOf(60.0, 11.0)
        )
        val p4inside = arrayOf(
            doubleArrayOf(51.0, 14.0),
            doubleArrayOf(52.0, 14.0),
            doubleArrayOf(52.0, 12.0),
            doubleArrayOf(51.0, 12.0)
        )
        GeoGeometry.overlap(polygon, polygon) shouldBe true
        GeoGeometry.overlap(polygon, p2overlapping) shouldBe true
        GeoGeometry.overlap(p2overlapping, polygon) shouldBe true
        GeoGeometry.overlap(p3outside, polygon) shouldBe false
        GeoGeometry.overlap(polygon, p4inside) shouldBe true
        GeoGeometry.overlap(p4inside, polygon) shouldBe true
    }

    @Test
    fun shouldCalculateSubHashesForHash() {
        val hash = "u33dbfc"
        val subHashes = GeoHashUtils.subHashes(hash)
        subHashes.size shouldBe 32
    }

    private fun assertSimilar(d1: Double, d2: Double) {
        // allow for some margin of error
        // should be similar$d1 and $d2
        abs(d1 - d2) shouldBeLessThan 0.0000001
    }
}
