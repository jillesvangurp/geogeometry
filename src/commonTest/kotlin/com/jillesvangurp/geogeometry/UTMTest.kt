package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.*
import com.jillesvangurp.geo.GeoGeometry.Companion.roundDecimals
import com.jillesvangurp.geojson.distanceTo
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import com.jillesvangurp.geojson.stringify
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random
import kotlin.test.Test

class UTMTest {

    @Test
    fun shouldConvertCoordinates() {
        val utmBbg = "33 U 389880.94 5819700.41"
        brandenBurgerGate.toUTM().toString() shouldBe utmBbg
        val utmAsWgs84 = utmBbg.utmAsWgs84Coordinate
        println(utmAsWgs84?.stringify())
        utmAsWgs84!! shouldBeNear brandenBurgerGate
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
        listOf(
            "33    U   3898.111111      5819",
            "33U 3898 5819",
            "33 U 389880.94 5819700.4"
        ).forEach { str ->
            str.parseUTM() shouldNotBe null
        }
    }

    @Test
    fun parseValidUtmString() {
        val utmString = "17 T 630084 4833438"
        val expected = UtmCoordinate(17, 'T', 630084.0, 4833438.0)
        val result = utmString.parseUTM()
        result shouldBe expected
    }

    @Test
    fun parseInvalidUtmString() {
        val utmString = "invalid"
        val result = utmString.parseUTM()
        result shouldBe null
    }

    @Test
    fun findMultipleCoordinates() {
        val textWithUtm = "Here are two UTM coordinates: 17 T 630084 4833438 and 18 S 233445 1948392."
        val expected = listOf(
            UtmCoordinate(17, 'T', 630084.0, 4833438.0),
            UtmCoordinate(18, 'S', 233445.0, 1948392.0)
        )
        val result = textWithUtm.findUTMCoordinates()
        result shouldBe expected
    }

    @Test
    fun noCoordinates() {
        val textWithoutUtm = "This text has no UTM coordinates."
        val result = textWithoutUtm.findUTMCoordinates()
        result shouldBe emptyList()
    }

    @Test
    fun edgeCases() {
        val testCoordinates = listOf(
            doubleArrayOf(0.0, 0.0), // Null Island
            doubleArrayOf(-0.0014, 51.4778), // Greenwich Observatory
            doubleArrayOf(0.0, 84.0), // max supported north lat
            doubleArrayOf(0.0, -80.0), // max supported south lat
            // Add more test coordinates as needed
        )
        val marginOfError = 1.0 // meters, adjust as per the precision requirement

        assertSoftly {
            testCoordinates.forEach { original ->
                withClue(original) {
                    val utm = original.toUTM()
                    val converted = utm.toWgs84()
                    val distance =
                        GeoGeometry.distance(original.latitude, original.longitude, converted[1], converted[0])

                    distance shouldBeLessThanOrEqual marginOfError
                }
            }
        }
    }

    @Test
    fun testLotsOfUtmCoordinates() {
        fun Random.supportedUtmCoordinate(): DoubleArray {
            return doubleArrayOf(
                nextDouble(-180.0, 180.0).roundDecimals(4),
                nextDouble(-80.0, 84.0).roundDecimals(4)
            )
        }

        assertSoftly {
            repeat(10000) {
                Random.supportedUtmCoordinate().let { p ->
                    val toUTM = p.toUTM()
                    runCatching {
                        val convertedBack = toUTM.toWgs84()
                        withClue("${p.stringify()} -> ${convertedBack.stringify()} - $toUTM") {
                            convertedBack.let { out ->
                                out.distanceTo(p) shouldBeLessThan 1.0
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun unSupportedCoordinates() {
        val testCoordinates = listOf(
            doubleArrayOf(0.0, 90.0), // North Pole
            doubleArrayOf(0.0, -90.0), // South Pole
            doubleArrayOf(10.0, -80.01), // outside supported range
            doubleArrayOf(10.0, 84.01), // outside supported range
        )

        assertSoftly {
            testCoordinates.forEach { point ->
                withClue(point) {
                    shouldThrowAny {
                        point.toUTM()
                    }
                }
            }
        }
    }
}
