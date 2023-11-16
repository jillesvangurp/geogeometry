package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.convertUTMToMGRS
import com.jillesvangurp.geo.toUtmCoordinate
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MGRSTest {
    @Test
    fun shouldCalculateMgrsForBrandenburgerTor() {
        assertSoftly {
            brandenBurgerGate.toUtmCoordinate().convertUTMToMGRS().let {
                withClue(it) {
                    it.longitudeZone shouldBe 33
                    it.latitudeZoneLetter shouldBe 'U'
                    it.eastingLetter shouldBe 'U'
                    it.northingLetter shouldBe 'U'
                    it.easting shouldBe 89880
                    it.northing shouldBe 19700
                }

            }
            val skagen = doubleArrayOf(10.591979, 57.724205)
            skagen.toUtmCoordinate().convertUTMToMGRS().let {
                withClue(it) {
                    it.longitudeZone shouldBe 32
                    it.latitudeZoneLetter shouldBe 'V'
                    it.eastingLetter shouldBe 'N'
                    it.northingLetter shouldBe 'J'
                    it.easting shouldBe 94817
                    it.northing shouldBe 99119
                }
            }
        }
    }
}