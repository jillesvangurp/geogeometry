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

        utm.convertUTMToMGRS().let { mgrs ->
            val toUtm = mgrs.toUtm()
            println(mgrs)
            withClue("$utm -> $mgrs -> $toUtm") {
                mgrs.longitudeZone shouldBe utm.longitudeZone
                mgrs.latitudeZoneLetter shouldBe utm.latitudeZoneLetter

                abs(utm.easting-toUtm.easting) shouldBeLessThan 2.0
                abs(utm.northing-toUtm.northing) shouldBeLessThan 2.0
//                toUtm.toPointCoordinates().distanceTo(p) shouldBeLessThan 2.0
            }
        }
    }

    @Test
    fun shouldTestMgrsParsing() {
        data class TestStrings(val input: String, val expected: String?)
        val fixture = listOf(
            TestStrings("23K PQ 82383 65269","23K PQ 82383 65269"),
            TestStrings("23K PQ 8238 6526","23K PQ 82380 65260"),
            TestStrings("23K PQ 823 652","23K PQ 82300 65200"),
            TestStrings("23K PQ 82 65","23K PQ 82000 65000"),
            TestStrings("23K PQ 8 6","23K PQ 80000 60000"),
            TestStrings("23KPQ8238365269","23K PQ 82383 65269"),
            TestStrings("23KPQ82386526","23K PQ 82380 65260"),
            TestStrings("23KPQ823652","23K PQ 82300 65200"),
            TestStrings("23KPQ8265","23K PQ 82000 65000"),
            TestStrings("23KPQ86","23K PQ 80000 60000"),
        )
        assertSoftly {
            fixture.forEach { testString ->
                withClue(testString.input) {
                    testString.input.parseMgrs()?.usng() shouldBe testString.expected
                }
            }
        }
    }
}