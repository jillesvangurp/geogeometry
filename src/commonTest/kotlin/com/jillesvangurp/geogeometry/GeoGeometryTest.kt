package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.GeoGeometry.Companion.changeOrder
import com.jillesvangurp.geo.GeoGeometry.Companion.ensureFollowsRightHandSideRule
import com.jillesvangurp.geo.GeoGeometry.Companion.hasSameStartAndEnd
import com.jillesvangurp.geo.GeoGeometry.Companion.isValid
import com.jillesvangurp.geo.GeoGeometry.Companion.roundToDecimals
import com.jillesvangurp.geojson.Geometry
import com.jillesvangurp.geojson.degree
import com.jillesvangurp.geojson.eastOrWest
import com.jillesvangurp.geojson.ensureFollowsRightHandSideRule
import com.jillesvangurp.geojson.humanReadable
import com.jillesvangurp.geojson.latLon
import com.jillesvangurp.geojson.lonLat
import com.jillesvangurp.geojson.minutes
import com.jillesvangurp.geojson.seconds
import com.jillesvangurp.serializationext.DEFAULT_JSON
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlinx.serialization.json.JsonObject

//    val bigRing = arrayOf(potsDammerPlatz, brandenBurgerGate, naturkundeMuseum, senefelderPlatz, moritzPlatz, potsDammerPlatz)
//    val smallRing = arrayOf(rosenthalerPlatz, oranienburgerTor, bergstr16Berlin, rosenthalerPlatz)

val sydneyOpera = lonLat(151.213108, -33.8567844)
val rioFootballStadium = lonLat(-43.2216922, -22.910643)

class GeoGeometryTest {

    @Test
    fun shouldConvertToDecimalDegree() {
        data class TestDegree(
            val direction: String,
            val degrees: Int,
            val minutes: Int,
            val seconds: Double,
            val decimalDegree: Double,
        )

        val degrees = listOf(
            TestDegree("W", 111, 38, 45.40, -111.64594444444445),
            TestDegree("E", 111, 38, 45.40, 111.64594444444445)
        )
        degrees.forEach { (direction, degrees, minutes, seconds, decimalDegrees) ->

            decimalDegrees.eastOrWest.letter.toString() shouldBe direction
            decimalDegrees.degree shouldBe degrees
            decimalDegrees.minutes shouldBe minutes
            roundToDecimals(decimalDegrees.seconds, 2) shouldBe seconds

            val decimalDegree = GeoGeometry.toDecimalDegree(direction, degrees, minutes, seconds)
            abs(decimalDegree - decimalDegrees) shouldBeLessThan 0.00001
        }
    }

    @Test
    fun shouldProduceHumanReadableDegree() {
        bergstr16Berlin.humanReadable() shouldBe """52° 31' 47.39" N, 13° 23' 39.03" E"""
        rioFootballStadium.humanReadable() shouldBe """22° 54' 38.31" S, 43° 13' 18.09" W"""
        sydneyOpera.humanReadable() shouldBe """33° 51' 24.42" S, 151° 12' 47.19" E"""
    }

    @Test
    fun shouldBeValidPolygon() {
        bigRing.hasSameStartAndEnd() shouldBe true
        smallRing.hasSameStartAndEnd() shouldBe true

        arrayOf(bigRing).isValid() shouldBe false
        arrayOf(bigRing.changeOrder()).isValid() shouldBe true

        arrayOf(smallRing).isValid() shouldBe false
        arrayOf(smallRing.changeOrder()).isValid() shouldBe true

        arrayOf(bigRing, smallRing.changeOrder()).isValid() shouldBe false
        arrayOf(bigRing.changeOrder(), smallRing).isValid() shouldBe true
        arrayOf(bigRing.changeOrder(), smallRing.changeOrder()).isValid() shouldBe false

        arrayOf(bigRing, smallRing.changeOrder()).ensureFollowsRightHandSideRule().isValid() shouldBe true
        arrayOf(bigRing.changeOrder(), smallRing).ensureFollowsRightHandSideRule().isValid() shouldBe true
        arrayOf(bigRing.changeOrder(), smallRing.changeOrder()).ensureFollowsRightHandSideRule().isValid() shouldBe true
    }

    @Test
    fun shouldBeValid() {
        val polygon = DEFAULT_JSON.decodeFromString(Geometry.serializer(), badGeo) as Geometry.Polygon
        polygon.coordinates?.isValid() shouldBe false
        (polygon.ensureFollowsRightHandSideRule() as Geometry.Polygon).coordinates?.isValid() shouldBe true
    }

    @Test
    fun shouldSerializeToSame() {
        val polygonObject = DEFAULT_JSON.decodeFromString(JsonObject.serializer(), testPolygon)
        val polygon = DEFAULT_JSON.decodeFromJsonElement(Geometry.serializer(), polygonObject) as Geometry.Polygon
        val serializedPolygonObject = DEFAULT_JSON.encodeToJsonElement(Geometry.serializer(), polygon)

        polygonObject shouldBe serializedPolygonObject
    }

    @Test
    fun headingFromTwoPoints() {
        GeoGeometry.headingFromTwoPoints(
            lonLat(13.0, 52.0),
            lonLat(14.0, 53.0)
        ).roundToLong() shouldBe 31
        GeoGeometry.headingFromTwoPoints(
            lonLat(14.0, 53.0),
            lonLat(13.0, 52.0)
        ).roundToLong() shouldBe 212
    }

    @Test
    fun headingFromTwoPointsShouldBeBetweenZeroAnd360() {
        GeoGeometry.headingFromTwoPoints(
            lonLat(13.0, 52.0),
            lonLat(13.0, 52.0001)
        ).roundToLong() shouldBe 0
        GeoGeometry.headingFromTwoPoints(
            lonLat(12.999, 52.0),
            lonLat(13.0, 52.0)
        ).roundToLong() shouldBe 90
        GeoGeometry.headingFromTwoPoints(
            lonLat(13.0, 52.0001),
            lonLat(13.0, 52.0)
        ).roundToLong() shouldBe 180
        GeoGeometry.headingFromTwoPoints(
            lonLat(13.0, 52.0),
            lonLat(12.999, 52.0)
        ).roundToLong() shouldBe 270
    }

    @Test
    fun shouldAllParseCorrectly() {
        assertSoftly {
            mapOf(
                // Valid cases with variations in spacing
                "40.7128, -74.0060" to latLon(40.7128, -74.0060),
                "  40.7128, -74.0060   " to latLon(40.7128, -74.0060),
                "40.7128,-74.0060" to latLon(40.7128, -74.0060),
                "40.7128 , -74.0060" to latLon(40.7128, -74.0060),
                "40.7128  ,   -74.0060" to latLon(40.7128, -74.0060),
                "48.8566, 2.3522" to latLon(48.8566, 2.3522),
                "0, 0" to latLon(0.0, 0.0),
                "-90, -180" to latLon(-90.0, -180.0),
                "90, 180" to latLon(90.0, 180.0),
                "-90.0, -180.0" to latLon(-90.0, -180.0),
                "90.0, 180.0" to latLon(90.0, 180.0),

                // Edge bounds
                "90, 180" to latLon(90.0, 180.0),
                "-90.0, -180.0" to latLon(-90.0, -180.0),

                // Whitespace trimming
                "   40.7128  ,  -74.0060 " to latLon(40.7128, -74.0060),

                // Invalid cases: out-of-bounds
                "91, 0" to null,
                "-91, 0" to null,
                "0, 181" to null,
                "0, -181" to null,
                "90.0001, 0" to null,
                "0, 180.0001" to null,
                "-91.0, 0" to null,
                "0, -181" to null,

                // Invalid cases: format issues
                "" to null,
                "   " to null,
                "," to null,
                "51.0500;13.7373" to null,
                "51.0500 13.7373" to null,
                "51.0500,13.7373,7.0" to null,
                "hello, world" to null,
                "not a coordinate" to null,
                "12.3" to null,
                "51.0500" to null,
                "51.0500," to null,
                ",13.7373" to null,
                "13.7373, 51.0500, extra" to null,
                "NaN, NaN" to null,
                "Infinity, -Infinity" to null,
                "--40.7128, -74.0060" to null,
                "40.7128,, -74.0060" to null,
                "40.7128, -74.0060," to null,
            ).forEach { (input, expected) ->
                val parsed = GeoGeometry.parseCoordinate(input)
                parsed shouldBe expected
                GeoGeometry.isValidCoordinate(input) shouldBe (parsed != null)
            }
        }
    }

    @Test
    fun shouldFindOnlyValidCoordinates() {
        val textWithValidAndInvalid = """
            Some text 40.7128, -74.0060 here, 
            then bad: 91.0, 0 and -91, 0, then 48.8566, 2.3522 again, 
            and 0,0 at the end.
            52.0,13.0,15,, coordinate with altitude
            185,13
        """.trimIndent()

        GeoGeometry.findAllCoordinates(textWithValidAndInvalid) shouldContainExactly listOf(
            lonLat(-74.0060, 40.7128),
            lonLat(2.3522, 48.8566),
            lonLat(0.0, 0.0),
            lonLat(13.0,52.0)
        )

        // Only invalid
        GeoGeometry.findAllCoordinates("junk 91,0; 1000,1000 text") shouldBe emptyList()

        // Empty input
        GeoGeometry.findAllCoordinates("") shouldBe emptyList()
    }
}

