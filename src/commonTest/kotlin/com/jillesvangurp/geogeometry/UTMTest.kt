package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.*
import com.jillesvangurp.geo.GeoGeometry.Companion.roundDecimals
import com.jillesvangurp.geojson.*
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.Test

data class TestCase(
    val name: String,
    val point: PointCoordinates,
    val utmString: String,
    val notes: String = "",
    val include: Boolean = true,
)

val TestCase.utm get() = utmString.parseUTM() ?: error("$utmString does not parse as UTM")

val testCasses = listOf(
    // I used https://coordinates-converter.com to verify all of these
    // convenient but not clear what implementation they use or how that has been validated
    TestCase(
        name = "Svalbard Museum",
        point = doubleArrayOf(15.652313, 78.222378),
        utmString = "33X 514863.281 8683270.114",
    ),
    TestCase(
        "Oslo, City Hall",
        doubleArrayOf(10.733866394995035, 59.912415229244004),
        "32V 596959.207 6642926.686"
    ),
    TestCase(
        name = "Brandenburgertor",
        point = brandenBurgerGate,
        utmString = "33U 389880.937 5819700.412"
    ),
    TestCase(
        name = "North Cape",
        doubleArrayOf(25.783432, 71.169817),
        "35W 456177.242 7896776.959"
    )
)

class UTMTest {

    @Test
    fun shouldConvertCoordinates() {
        val utmBbg = "33 U 389880.94 5819700.41"
        brandenBurgerGate.toUtmCoordinate().toString() shouldBe utmBbg
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
                    val utm = original.toUtmCoordinate()
                    val converted = utm.utmToPointCoordinates()
                    val distance =
                        GeoGeometry.distance(original.latitude, original.longitude, converted[1], converted[0])

                    distance shouldBeLessThanOrEqual marginOfError
                }
            }
        }
    }

    @Test
    fun testLotsOfUtmCoordinates() {
        // important test that verifies that we can convert our own utm coordinates back consistently
        // it might still be wrong but at least we're being consistent :-)
        fun Random.supportedUtmCoordinate(): DoubleArray {
            return doubleArrayOf(
                nextDouble(-180.0, 180.0).roundDecimals(4),
                nextDouble(-80.0, 84.0).roundDecimals(4)
            )
        }

        assertSoftly {
            repeat(100000) {
                Random.supportedUtmCoordinate().let { p ->
                    val toUTM = p.toUtmCoordinate()

                    runCatching {
                        val convertedBack = toUTM.utmToPointCoordinates()
                        withClue("${p.stringify()} -> ${convertedBack.stringify()} - $toUTM") {
                            convertedBack.distanceTo(p) shouldBeLessThan 1.0
                        }
                    }
                    
                    val toMgrs = toUTM.toMgrs()
                    withClue("${p.latitude},${p.longitude} $toMgrs") {
                        toMgrs.toString().parseMgrs()!!.toPointCoordinate().distanceTo(p) shouldBeLessThan 2.0
                    }

                    val newUtm = toMgrs.toUtm()
                    withClue("${p.latitude},${p.longitude} $toUTM | $toMgrs | $newUtm") {
                        // rounding errors can add up to 2 meters but close enough
                        newUtm.toPointCoordinates().distanceTo(p) shouldBeLessThan 2.0
                    }
                }
            }
        }
    }

    @Test
    fun testLotsOfUpsCoordinates() {
        // important test that verifies that we can convert our own utm coordinates back consistently
        // it might still be wrong but at least we're being consistent :-)
        fun Random.supportedUpsCoordinate(): DoubleArray {
            return doubleArrayOf(
                nextDouble(-180.0, 180.0).roundDecimals(4),
                if (nextBoolean()) nextDouble(-90.0, -80.001).roundDecimals(4) else nextDouble(
                    84.001,
                    90.0
                ).roundDecimals(4)
            )
        }

        val letters = mutableSetOf<Char>()
        assertSoftly {
            repeat(100000) {
                Random.supportedUpsCoordinate().let { p ->
                    val toUTM = p.toUpsCoordinate()
                    letters.add(toUTM.latitudeZoneLetter)
                    runCatching {
                        val convertedBack = toUTM.upsToPointCoordinates()
                        withClue("${p.stringify()} -> ${convertedBack.stringify()} - $toUTM") {
                            convertedBack.distanceTo(p) shouldBeLessThan 1.0
                        }
                    }
                }
            }
        }
        println(letters)
    }

    @Test
    fun testLotsOfCoordinates() {
        // double check we are consistent across the utm and ups coordinate systems
        fun Random.supportedUpsCoordinate(): DoubleArray {
            return doubleArrayOf(
                nextDouble(-180.0, 180.0).roundDecimals(4),
                nextDouble(-90.0, 90.0).roundDecimals(4)
            )
        }

        assertSoftly {
            repeat(100000) {
                Random.supportedUpsCoordinate().let { p ->
                    val toUTM = p.toUtmOrUps()
                    runCatching {
                        val convertedBack = toUTM.toPointCoordinates()
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
                        point.toUtmCoordinate()
                    }
                }
            }
        }
    }

    @Test
    fun checkSvalbardAndNorwayExceptions() {
        assertSoftly {
            testCasses.forEach { testCase ->
                if(testCase.include) {
                    withClue(testCase.name) {
                        val utmCalculated = testCase.point.toUtmOrUps()
                        utmCalculated.longitudeZone shouldBe testCase.utm.longitudeZone
                        utmCalculated.latitudeZoneLetter shouldBe testCase.utm.latitudeZoneLetter
                        // allow for 10m wiggle room
                        assertSoftly {
                            withClue("easting") {
                                (utmCalculated.easting - testCase.utm.easting).absoluteValue shouldBeLessThan 10.0
                            }
                            withClue("northing") {
                                (utmCalculated.northing - testCase.utm.northing).absoluteValue shouldBeLessThan 10.0
                            }
                        }

                        println("${testCase.name} ${testCase.point.latitude},${testCase.point.longitude} -> ${utmCalculated.toMgrs()}")
                    }
                } else {
                    // reserve for test cases that aren't working
                    println("skipping ${testCase.name} because ${testCase.notes}")
                }
            }
        }
    }
}
