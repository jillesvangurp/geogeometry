package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.convertUTMToMGRS
import com.jillesvangurp.geo.toPointCoordinates
import com.jillesvangurp.geo.toUtm
import com.jillesvangurp.geo.toUtmCoordinate
import com.jillesvangurp.geojson.distanceTo
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MGRSTest {
    @Test
    fun shouldCalculateMgrsForBrandenburgerTor() {
        assertSoftly {
            var utm = brandenBurgerGate.toUtmCoordinate()
            utm.convertUTMToMGRS().let {
                withClue(it) {
                    it.longitudeZone shouldBe 33
                    it.latitudeZoneLetter shouldBe 'U'
                    it.colLetter shouldBe 'U'
                    it.rowLetter shouldBe 'U'
                    it.easting shouldBe 89880
                    it.northing shouldBe 19700
                    println(utm)
                    val toUtm = it.toUtm()
                    println(toUtm)
                    toUtm.toPointCoordinates().distanceTo(brandenBurgerGate) shouldBeLessThan 2.0
                }

            }
            val skagen = doubleArrayOf(10.591979, 57.724205)
            utm = skagen.toUtmCoordinate()
            utm.convertUTMToMGRS().let {
                withClue(it) {
                    it.longitudeZone shouldBe 32
                    it.latitudeZoneLetter shouldBe 'V'
                    it.colLetter shouldBe 'N'
                    it.rowLetter shouldBe 'J'
                    it.easting shouldBe 94817
                    it.northing shouldBe 99119

                    println(utm)
                    val toUtm = it.toUtm()
                    println(toUtm)
                    toUtm.toPointCoordinates().distanceTo(skagen) shouldBeLessThan 2.0
                }
            }
        }
    }
}