package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geo.latitude
import com.jillesvangurp.geo.longitude
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

    // @Test
    // fun shouldBreak() {
    //     42 shouldBe 40
    // }

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
}
