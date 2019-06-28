package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geo.latitude
import com.jillesvangurp.geo.longitude
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import kotlin.math.abs

class GeoHashUtilsTest : StringSpec() {
    val coordinatesWithHashes = arrayOf(
        row(0.1, -0.1, "ebpbtdpntc6e"),
        row(52.530888, 13.394904, "u33dbfcyegk2")
    )

    init {
        "decode hash" {
            forall(*coordinatesWithHashes) { lat, lon, geoHash ->
                val decoded = GeoHashUtils.decode(geoHash)
                decoded.latitude shouldBeApproximately lat
                decoded.longitude shouldBeApproximately lon
            }
        }

        "encode hash" {
            forall(*coordinatesWithHashes) { lat, lon, geoHash ->
                GeoHashUtils.encode(lat, lon) shouldBe geoHash
                GeoHashUtils.encode(doubleArrayOf(lon, lat)) shouldBe geoHash
            }
        }

        "hash bbox should contain coordinate" {
            forall(*coordinatesWithHashes) { lat, lon, geoHash ->
                GeoHashUtils.contains(geoHash, lat, lon) shouldBe true
                GeoHashUtils.contains(geoHash, lon, lat) shouldBe false
            }
        }

        "decode bbox" {
            forall(*coordinatesWithHashes) { lat, lon, geoHash ->
                val bbox = GeoHashUtils.decodeBbox(geoHash)

                abs((bbox[0] + bbox[1]) / 2 - lat) shouldBeApproximately 0.0
                abs((bbox[2] + bbox[3]) / 2 - lon) shouldBeApproximately 0.0
            }
        }
    }
}