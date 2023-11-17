package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.*
import com.jillesvangurp.geojson.distanceTo
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.test.Test

class MGRSTest {
    @Test
    fun shouldCalculateMgrsForBrandenburgerTor() {
        assertSoftly {
            testMgrsConversion(brandenBurgerGate)
            val skagen = doubleArrayOf(10.591979, 57.724205)
            testMgrsConversion(skagen)
            testMgrsConversion(rioFootballStadium)
            testMgrsConversion(sydneyOpera)
        }
    }

    private fun testMgrsConversion(p: DoubleArray) {
        val utm = p.toUtmCoordinate()
        utm.toPointCoordinates().distanceTo(p) shouldBeLessThan 2.0

        utm.convertUTMToMGRS().let {
            val toUtm = it.toUtm()
            withClue("$utm -> $it -> $toUtm") {
                it.longitudeZone shouldBe utm.longitudeZone
                it.latitudeZoneLetter shouldBe utm.latitudeZoneLetter

                abs(utm.easting-toUtm.easting) shouldBeLessThan 2.0
                abs(utm.northing-toUtm.northing) shouldBeLessThan 2.0
//                toUtm.toPointCoordinates().distanceTo(p) shouldBeLessThan 2.0
            }

        }

    }
}