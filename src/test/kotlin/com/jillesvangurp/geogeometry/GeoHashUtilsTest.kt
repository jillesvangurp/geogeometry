package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geo.latitude
import com.jillesvangurp.geo.longitude
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import kotlin.math.abs

class GeoHashUtilsTest : StringSpec() {
    val coordinatesWithHashes = arrayOf(
        row(0.1, -0.1, "ebpbtdpntc6e"),
        row(52.530888, 13.394904, "u33dbfcyegk2")
    )

    init {
        "decode hash" {
            forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
                val decoded = GeoHashUtils.decode(geoHash)
                decoded.latitude shouldBeApproximately lat
                decoded.longitude shouldBeApproximately lon
            }
        }

        "encode hash" {
            forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
                GeoHashUtils.encode(lat, lon) shouldBe geoHash
                GeoHashUtils.encode(doubleArrayOf(lon, lat)) shouldBe geoHash
            }
        }

        "hash bbox should contain coordinate" {
            forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
                GeoHashUtils.contains(geoHash, lat, lon) shouldBe true
                GeoHashUtils.contains(geoHash, lon, lat) shouldBe false
            }
        }

        "decode bbox" {
            forAll(*coordinatesWithHashes) { lat: Double, lon: Double, geoHash: String ->
                val bbox = GeoHashUtils.decodeBbox(geoHash)

                abs((bbox[0] + bbox[1]) / 2 - lat) shouldBeApproximately 0.0
                abs((bbox[2] + bbox[3]) / 2 - lon) shouldBeApproximately 0.0
            }
        }
    }
}
