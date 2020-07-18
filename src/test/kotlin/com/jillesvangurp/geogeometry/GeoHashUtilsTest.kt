package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry.Companion.distance
import com.jillesvangurp.geo.GeoGeometry.Companion.overlap
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geo.GeoHashUtils.Companion.decode
import com.jillesvangurp.geo.GeoHashUtils.Companion.decodeBbox
import com.jillesvangurp.geo.GeoHashUtils.Companion.east
import com.jillesvangurp.geo.GeoHashUtils.Companion.encode
import com.jillesvangurp.geo.GeoHashUtils.Companion.geoHashesForCircle
import com.jillesvangurp.geo.GeoHashUtils.Companion.geoHashesForPolygon
import com.jillesvangurp.geo.GeoHashUtils.Companion.isEast
import com.jillesvangurp.geo.GeoHashUtils.Companion.isWest
import com.jillesvangurp.geo.GeoHashUtils.Companion.west
import com.jillesvangurp.geo.eastLongitude
import com.jillesvangurp.geo.latitude
import com.jillesvangurp.geo.longitude
import com.jillesvangurp.geo.northLatitude
import com.jillesvangurp.geo.southLatitude
import com.jillesvangurp.geo.westLongitude
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.testng.Assert
import kotlin.math.abs

class GeoHashUtilsTest {
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
    fun `decode hash`() {
        forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
            val decoded = GeoHashUtils.decode(geoHash)
            decoded.latitude shouldBeApproximately lat
            decoded.longitude shouldBeApproximately lon
        }
    }

    @Test
    fun `encode hash`() {
        forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
            GeoHashUtils.encode(lat, lon) shouldBe geoHash
            GeoHashUtils.encode(doubleArrayOf(lon, lat)) shouldBe geoHash
        }
    }

    @Test
    fun `hash bbox should contain coordinate`() {
        forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
            GeoHashUtils.contains(geoHash, lat, lon) shouldBe true
            GeoHashUtils.contains(geoHash, lon, lat) shouldBe false
        }
    }

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
                    ), doubleArrayOf(longitude, latitude)
                ), Matchers.lessThan(500.0)
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
        Assert.assertTrue(overlap(polygon, polygon))
        Assert.assertTrue(overlap(polygon, p2overlapping))
        Assert.assertTrue(overlap(p2overlapping, polygon))
        Assert.assertFalse(overlap(p3outside, polygon))
        Assert.assertTrue(overlap(polygon, p4inside))
        Assert.assertTrue(overlap(p4inside, polygon))
    }
}
