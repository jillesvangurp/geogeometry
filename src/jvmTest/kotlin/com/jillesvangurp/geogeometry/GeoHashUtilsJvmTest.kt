package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.*
import com.jillesvangurp.geo.GeoGeometry.Companion.area
import com.jillesvangurp.geo.GeoGeometry.Companion.boundingBox
import com.jillesvangurp.geo.GeoGeometry.Companion.distance
import com.jillesvangurp.geo.GeoGeometry.Companion.overlap
import com.jillesvangurp.geo.GeoHashUtils.Companion.contains
import com.jillesvangurp.geo.GeoHashUtils.Companion.decode
import com.jillesvangurp.geo.GeoHashUtils.Companion.decodeBbox
import com.jillesvangurp.geo.GeoHashUtils.Companion.east
import com.jillesvangurp.geo.GeoHashUtils.Companion.encode
import com.jillesvangurp.geo.GeoHashUtils.Companion.geoHashesForCircle
import com.jillesvangurp.geo.GeoHashUtils.Companion.geoHashesForLine
import com.jillesvangurp.geo.GeoHashUtils.Companion.geoHashesForPolygon
import com.jillesvangurp.geo.GeoHashUtils.Companion.isEast
import com.jillesvangurp.geo.GeoHashUtils.Companion.isNorth
import com.jillesvangurp.geo.GeoHashUtils.Companion.isSouth
import com.jillesvangurp.geo.GeoHashUtils.Companion.isWest
import com.jillesvangurp.geo.GeoHashUtils.Companion.north
import com.jillesvangurp.geo.GeoHashUtils.Companion.south
import com.jillesvangurp.geo.GeoHashUtils.Companion.subHashes
import com.jillesvangurp.geo.GeoHashUtils.Companion.suitableHashLength
import com.jillesvangurp.geo.GeoHashUtils.Companion.west
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import kotlin.math.abs

class GeoHashUtilsJvmTest {
    val json = Json {}
    val coordinatesWithHashes = arrayOf(
        row(0.1, -0.1, "ebpbtdpntc6e"),
        row(52.530888, 13.394904, "u33dbfcyegk2")
    )

    fun lines() = arrayOf(
        arrayOf(1, 1, 2, 2),
        arrayOf(2, 2, 1, 1),
        arrayOf(2, 1, 1, 1),
        arrayOf(1, 2, 1, 1),
        arrayOf(1, 1, 2, 1),
        arrayOf(1, 1, 1, 2),
        arrayOf(1, 1, 1, 2)
    )

    @Test
    fun `decode bbox`() {
        forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
            val bbox = GeoHashUtils.decodeBbox(geoHash)

            abs((bbox.southLatitude + bbox.northLatitude) / 2 - lat) shouldBeApproximately 0.0
            abs((bbox.westLongitude + bbox.eastLongitude) / 2 - lon) shouldBeApproximately 0.0
        }
    }

    @Test
    fun shouldCalculateEastOn180() {
        val hash = encode(-18.0, 179.9, 3)
        var bbox = decodeBbox(hash)
        MatcherAssert.assertThat(
            bbox[2],
            CoreMatchers.equalTo(180.0)
        )
        val east = east(hash)
        bbox = decodeBbox(east)
        MatcherAssert.assertThat(
            bbox[0],
            CoreMatchers.equalTo(-180.0)
        )
    }

    @Test
    fun shouldCalculateWestOn180() {
        val hash = encode(-18.0, -179.9, 3)
        var bbox = decodeBbox(hash)
        MatcherAssert.assertThat(
            bbox[0],
            CoreMatchers.equalTo(-180.0)
        )
        val west = west(hash)
        bbox = decodeBbox(west)
        MatcherAssert.assertThat(
            bbox[2],
            CoreMatchers.equalTo(180.0)
        )
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
        val geoHashesForPolygon = geoHashesForPolygon(
            8, *polygon
        )
        for (h in geoHashesForPolygon) {
            min = Math.min(min, h.length)
        }
        MatcherAssert.assertThat(
            "there should be some hashes with length=3",
            min,
            CoreMatchers.`is`(3)
        )
        MatcherAssert.assertThat(
            "huge area, should generate lots of hashes",
            geoHashesForPolygon.size > 1000
        )
    }

    @Test
    fun shouldCalculateHashesForCircle() {
        val hashesForCircle =
            geoHashesForCircle(8, 52.0, 13.0, 2000.0)
        for (hash in hashesForCircle) {
            val point = decode(hash)
            val distance = distance(point, doubleArrayOf(13.0, 52.0))
            MatcherAssert.assertThat(
                distance,
                Matchers.lessThan(2000.0)
            )
        }
    }

    @Test
    fun shouldCheckIfWest() {
        MatcherAssert.assertThat("should be west", isWest(90.0, 91.0))
        MatcherAssert.assertThat("should be west", isWest(-1.0, 1.0))
        MatcherAssert.assertThat("should be west", isWest(-89.0, 90.0))
        MatcherAssert.assertThat("should be west", isWest(180.0, -178.0))
        MatcherAssert.assertThat("should be west", isWest(180.0, -179.99527198651967))
        MatcherAssert.assertThat("should not be west", !isWest(-179.0, 180.0))
        MatcherAssert.assertThat("should not be west", !isWest(91.0, 90.0))
        MatcherAssert.assertThat("should not be west", !isWest(-179.0, 180.0))
        MatcherAssert.assertThat("should not be west", !isWest(89.0, -90.0))
        MatcherAssert.assertThat("should not be west", !isWest(1.0, -1.0))
        MatcherAssert.assertThat("should not be west", !isWest(91.0, 90.0))
        MatcherAssert.assertThat("should not be west", !isWest(-91.0, 90.0))
    }

    @Test
    fun shouldCheckIfEast() {
        MatcherAssert.assertThat("should not be east", !isEast(90.0, 91.0))
        MatcherAssert.assertThat("should not be east", !isEast(-1.0, 1.0))
        MatcherAssert.assertThat("should not be east", !isEast(-89.0, 90.0))
        MatcherAssert.assertThat("should not be east", !isEast(180.0, -178.0))
        MatcherAssert.assertThat("should not be west", !isEast(180.0, -179.99527198651967))
        MatcherAssert.assertThat("should be east", isEast(-179.0, 180.0))
        MatcherAssert.assertThat("should be east", isEast(91.0, 90.0))
        MatcherAssert.assertThat("should be east", isEast(-179.0, 180.0))
        MatcherAssert.assertThat("should be east", isEast(89.0, -90.0))
        MatcherAssert.assertThat("should be east", isEast(1.0, -1.0))
        MatcherAssert.assertThat("should be east", isEast(91.0, 90.0))
        MatcherAssert.assertThat("should be east", isEast(-91.0, 90.0))
    }

    @Test
    fun shouldBeNeitherWestNorEast() {
        MatcherAssert.assertThat("should not be west", !isWest(-90.0, 90.0))
        MatcherAssert.assertThat("should not be east", !isEast(-90.0, 90.0))
    }

    @Test
    fun shouldGenerateCircleHashesThatAreAllWithinRadiusOfCircle() {
        val latitude = 52.529731
        val longitude = 13.401284
        val radius = 500
        val hashes =
            geoHashesForCircle(8, latitude, longitude, radius.toDouble())
        for (hash in hashes) {
            MatcherAssert.assertThat(
                distance(
                    decode(
                        hash
                    ),
                    doubleArrayOf(longitude, latitude)
                ),
                Matchers.lessThan(500.0)
            )
        }
        MatcherAssert.assertThat(hashes.size, Matchers.greaterThan(radius))
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
        overlap(polygon, polygon) shouldBe true
        overlap(polygon, p2overlapping) shouldBe true
        overlap(p2overlapping, polygon) shouldBe true
        overlap(p3outside, polygon) shouldBe false
        overlap(polygon, p4inside) shouldBe true
        overlap(p4inside, polygon) shouldBe true
    }

    val samplePoints = arrayOf<Array<Double>>(
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
    )

    @Test
    fun shouldCalculateHashLength() {
        samplePoints.forEach { (m, latitude, longitude) ->
            val length = suitableHashLength(m, latitude, longitude)
            val hash = encode(latitude, longitude, length)
            val bbox = decodeBbox(hash)
            val distance = distance(bbox[0], bbox[1], bbox[0], bbox[3])
            MatcherAssert.assertThat(distance, Matchers.lessThan(m))
        }
    }

    val lines = arrayOf(
        arrayOf(1, 1, 2, 2),
        arrayOf(2, 2, 1, 1),
        arrayOf(2, 1, 1, 1),
        arrayOf(1, 2, 1, 1),
        arrayOf(1, 1, 2, 1),
        arrayOf(1, 1, 1, 2),
        arrayOf(1, 1, 1, 2)
    )

    @Test
    fun shouldCalculateHashesForLine() {
        lines.forEach { (lat1, lon1, lat2, lon2) ->

            val hashes = geoHashesForLine(
                10000.0, lat1.toDouble(), lon1.toDouble(), lat2.toDouble(),
                lon2.toDouble()
            )
//        GsonBuilder b = new GsonBuilder();
//        Gson gson = b.serializeNulls().create();
//
//
//        PointGeometry p1 = PointGeometry.of(lon1, lat1);
//        PointGeometry p2 = PointGeometry.of(lon2, lat2);
//        double[][] line = p1.line(p2);
//        LineStringGeometry lineGeo = new LineStringGeometry(line,null);
//        System.out.println(gson.toJson(
//                FeatureCollection.of(p1.asFeature(null,null), p2.asFeature(null,null), lineGeo.asFeature(null,null)).plus( FeatureCollection.fromGeoHashes(hashes))));
            MatcherAssert.assertThat(
                "number of hashes, orientation",
                hashes.size,
                Matchers.greaterThan(10)
            )
        }
    }

    //    @Test(enabled = false)
    //    public void shouldCalculateBboxSizes() {
    //        System.out.println("<table border=\"1\">");
    //        System.out.println("<th><td>latitude</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td><td>7</td><td>8</td><td>9</td><td>10</td><td>11</td><td>12</td></th>");
    //        printHashSizes(90, 0);
    //        printHashSizes(80, 0);
    //        printHashSizes(70, 0);
    //        printHashSizes(60, 0);
    //        printHashSizes(50, 0);
    //        printHashSizes(40, 0);
    //        printHashSizes(30, 0);
    //        printHashSizes(20, 0);
    //        printHashSizes(10, 0);
    //        printHashSizes(0, 0);
    //        System.out.println("</table>");
    //    }
    //    private void printHashSizes(double lat, double lon) {
    //        String geoHash = encode(lat, lon, DEFAULT_GEO_HASH_LENGTH);
    //
    //        // not a test but nice to get a sense of the scale of a geo hash
    //        System.out.println("<tr><td>" + lat + "</td>");
    //        for (int i = 1; i <= geoHash.length(); i++) {
    //            String prefix = geoHash.substring(0, i);
    //            double[] bbox = decodeBbox(prefix);
    //            double vertical = roundToDecimals(distance(bbox[0], bbox[3], bbox[1], bbox[3]), 2);
    //            double horizontal = roundToDecimals(distance(bbox[0], bbox[2], bbox[0], bbox[3]), 2);
    //            System.out.print("<td>" + horizontal + "x" + vertical + "</td>");
    //        }
    //        System.out.print("</tr>\n");
    //    }

    @Test
    fun shouldCalculateSubHashesForHash() {
        val hash = "u33dbfc"
        val subHashes = subHashes(hash)
        subHashes.size shouldBe 32
        val first = subHashes[0]
        var row = first
        for (j in 0..15) {
            var column = row
            for (i in 0..7) {
                column = east(column)
            }
            row = north(row)
        }
    }

    val coordinates = listOf(
        doubleArrayOf(-0.1, 0.1) to "ebpbtdpntc6e",
        doubleArrayOf(13.394904, 52.530888) to "u33dbfcyegk2"
    )

    @Test
    fun shouldDecode() {
        io.kotest.assertions.assertSoftly {
            coordinates.forEach { (point, geoHash) ->

                val decoded = decode(geoHash)

                assertSimilar(point.latitude, decoded[1])
                assertSimilar(point.longitude, decoded[0])
            }
        }
    }

    @Test
    fun shouldEncode() {
        coordinates.forEach { (point, geoHash) ->

            MatcherAssert.assertThat(
                encode(
                    point.latitude,
                    point.longitude,
                    GeoHashUtils.DEFAULT_GEO_HASH_LENGTH
                ),
                CoreMatchers.`is`(geoHash)
            )
            MatcherAssert.assertThat(
                encode(point),
                CoreMatchers.`is`(geoHash)
            )
        }
    }

    @Test
    fun shouldContainCoordinate() {
        coordinates.forEach { (point, geoHash) ->
            MatcherAssert.assertThat(
                "hash should contain the coordinate",
                contains(geoHash, point.latitude, point.longitude)
            )
            MatcherAssert.assertThat(
                "hash should not contain the swapped coordinate",
                !contains(geoHash, point.longitude, point.latitude)
            )
        }
    }

    @Test
    fun shouldDecodeBbox() {
        coordinates.forEach { (point, geoHash) ->

            val bbox = decodeBbox(geoHash)
            MatcherAssert.assertThat(
                abs((bbox[0] + bbox[2]) / 2 - point.longitude),
                Matchers.lessThan(0.0001)
            )
            MatcherAssert.assertThat(
                abs((bbox[1] + bbox[3]) / 2 - point.latitude),
                Matchers.lessThan(0.0001)
            )
        }
    }

    @Test
    fun shouldCalculateEast() {
        coordinates.forEach { (point, geoHash) ->

            val original = geoHash.substring(0, 3)
            val calculated = east(geoHash.substring(0, 3))
            MatcherAssert.assertThat(
                "east hash should not contain the coordinate",
                !contains(calculated, point.latitude, point.longitude)
            )
            val bbox = decodeBbox(original)
            val eastBbox = decodeBbox(calculated)
            assertSimilar(bbox[1], eastBbox[1])
            assertSimilar(bbox[3], eastBbox[3])
            assertSimilar(bbox[2], eastBbox[0])
            assertSimilar((eastBbox[2] - bbox[0]) / 2, bbox[2] - bbox[0])
            val nl = decode(calculated)[0]
            val ol = decode(original)[0]
            MatcherAssert.assertThat(
                "decoded hash lon should be east of original",
                isEast(nl, ol)
            )
        }
    }

    @Test
    fun shouldCalculateSouth() {
        coordinates.forEach { (point, geoHash) ->

            val original = geoHash.substring(0, 3)
            val calculatedHash = south(original)
            //        System.out.println(original + " " + calculatedHash);
            val oBox = decodeBbox(original)
            val cBox = decodeBbox(calculatedHash)
            MatcherAssert.assertThat(
                "calculated hash should not contain the coordinate",
                !contains(calculatedHash, point.latitude, point.longitude)
            )
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
            val nl = decode(calculatedHash)[1]
            val ol = decode(original)[1]
            MatcherAssert.assertThat(
                "decoded hash lat should be south of original",
                isSouth(nl, ol)
            )
        }
    }

    @Test
    fun shouldCalculateNorth() {
        coordinates.forEach { (point, geoHash) ->
            val original = geoHash.substring(0, 3)
            val calculatedHash = north(original)
            val oBox = decodeBbox(original)
            val cBox = decodeBbox(calculatedHash)
            MatcherAssert.assertThat(
                "calculated hash should not contain the coordinate",
                !contains(calculatedHash, point.latitude, point.longitude)
            )
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
            val nl = decode(calculatedHash)[1]
            val ol = decode(original)[1]
            MatcherAssert.assertThat(
                "decoded hash lat should be north of original",
                isNorth(nl, ol)
            )
        }
    }

    @Test
    fun shouldCalculateWest() {
        coordinates.forEach { (point, geoHash) ->

            val original = geoHash.substring(0, 3)
            val calculatedHash = west(original)
            val oBox = decodeBbox(original)
            val cBox = decodeBbox(calculatedHash)
            MatcherAssert.assertThat(
                "calculated hash should not contain the coordinate",
                !contains(calculatedHash, point.latitude, point.longitude)
            )
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
            val nl = decode(calculatedHash)[0]
            val ol = decode(original)[0]
            MatcherAssert.assertThat(
                "decoded hash lon should be west of original",
                isWest(nl, ol)
            )
        }
    }

    private fun assertSimilar(d1: Double, d2: Double) {
        // allow for some margin of error
        MatcherAssert.assertThat(
            "should be similar$d1 and $d2", Math.abs(d1 - d2),
            Matchers.lessThan(0.0000001)
        )
    }

    @Test
    fun `should cover concave polygon with hashes`() {
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
        val p = json.decodeFromString(Geometry.serializer(), concavePolygon) as Geometry.PolygonGeometry
        val hashes = GeoHashUtils.geoHashesForPolygon(*p.coordinates?.get(0) ?: throw IllegalStateException())


        println(json.encodeToString(FeatureCollection.serializer(), FeatureCollection.fromGeoHashes(hashes)))

        val area = hashes.map { GeoHashUtils.decodeBbox(it) }.map { area(it) }.sum()
        val bboxArea = area(boundingBox(p.coordinates as PolygonCoordinates))
        // it's a concave polygon so the area of the hashes should be much smaller than that of the
        // bounding box containing the polygon
        area shouldBeLessThan bboxArea*0.6

    }
}
