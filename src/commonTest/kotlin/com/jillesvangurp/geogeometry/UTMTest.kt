package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class UTMTest {

    @Test
    fun shouldConvertCoordinates() {
        brandenBurgerGate.toUTM().toString() shouldBe "33 U 389880.94 5819700.4"
        brandenBurgerGate.format shouldBe "52.516279N 13.377157E"
        "33 U 389880.94 5819700.4".utmAsWgs84!! shouldBeNear  brandenBurgerGate
    }

    @Test
    fun shouldDetectUtmCoordinatesInStrings() {
        val utmCoordinateStrings = listOf(
            "33 U 389880.94 5819700.4 bla bla",
            "bla bla 33\tU\t\t389880.94\t\t5819700.4",
            "33U 3898 5819"
        )
        utmCoordinateStrings.forEach { str ->
            str.findUTMCoordinates().size shouldBe 1
        }

        utmCoordinateStrings.joinToString(" ").findUTMCoordinates().size shouldBe utmCoordinateStrings.size
    }


    @Test
    fun shouldParseUtmCoordinates() {
        val utmCoordinates = listOf(
            "33    U   3898.111111      5819",
            "33U 3898 5819",
            "33 U 389880.94 5819700.4"
        ).forEach { str ->
            str.parseUTM() shouldNotBe null
        }
    }
}
