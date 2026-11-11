package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry.Companion.polygonContains
import com.jillesvangurp.geojson.LinearRingCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import com.jillesvangurp.geojson.lonLat
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.test.Test

class PolygonContainsTest {
    @Test
    fun shouldContainPointsInAConvexDiamond() {
        val diamond = arrayOf(
            lonLat(0.0, 3.0),
            lonLat(4.0, 1.0),
            lonLat(8.0, 3.0),
            lonLat(4.0, 5.0)
        )

        checkContainmentOnGrid(diamond) { x, y ->
            abs(x - 4.0) + 2.0 * abs(y - 3.0) <= 4.0
        }
    }

    @Test
    fun shouldContainPointsInAConcaveRingWithARectangularNotch() {
        val ring = arrayOf(
            lonLat(0.0, 0.0),
            lonLat(9.0, 0.0),
            lonLat(9.0, 6.0),
            lonLat(6.0, 6.0),
            lonLat(6.0, 2.0),
            lonLat(2.0, 2.0),
            lonLat(2.0, 6.0),
            lonLat(0.0, 6.0)
        )

        checkContainmentOnGrid(ring) { x, y ->
            // The base and both arms include their shared boundaries.
            (x in 0.0..9.0 && y in 0.0..2.0) ||
                (x in 0.0..2.0 && y in 2.0..6.0) ||
                (x in 6.0..9.0 && y in 2.0..6.0)
        }
    }

    @Test
    fun shouldContainPointsInAConcaveRingWithADiagonalNotch() {
        val ring = arrayOf(
            lonLat(0.0, 0.0),
            lonLat(9.0, 0.0),
            lonLat(9.0, 6.0),
            lonLat(6.0, 6.0),
            lonLat(4.0, 2.0),
            lonLat(2.0, 6.0),
            lonLat(0.0, 6.0)
        )

        checkContainmentOnGrid(ring) { x, y ->
            x in 0.0..9.0 && y in 0.0..6.0 && y <= 2.0 + 2.0 * abs(x - 4.0)
        }
    }

    private fun checkContainmentOnGrid(
        fixture: LinearRingCoordinates,
        expectedContains: (x: Double, y: Double) -> Boolean
    ) {
        // Translate the fixtures so latitude and longitude occupy distinct ranges.
        val west = -73.0
        val south = 42.0
        val open = fixture.map { lonLat(west + it.longitude, south + it.latitude) }.toTypedArray()
        val reversed = open.reversedArray()
        val rings = listOf(
            "open" to open,
            "closed" to (open + open.first()),
            "reversed open" to reversed,
            "reversed closed" to (reversed + reversed.first())
        )

        for (xStep in -2..20) {
            for (yStep in -2..14) {
                val x = xStep / 2.0
                val y = yStep / 2.0
                val point = lonLat(west + x, south + y)
                val expected = expectedContains(x, y)
                for ((description, ring) in rings) {
                    withClue("$description ring at local point [$x, $y]") {
                        polygonContains(point, ring) shouldBe expected
                    }
                }
            }
        }
    }
}
