package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.calculateAngle
import com.jillesvangurp.geo.calculateHeadingDifference
import com.jillesvangurp.geojson.*
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.*
import kotlin.test.Test

class RotationTest {

    @Test
    fun shouldTranslateCircle() {
        val circle = GeoGeometry.circle2polygon(20, rosenthalerPlatz.latitude, rosenthalerPlatz.longitude, 20.0)
            .polygonGeometry()

        val moved = circle.translate(oranienburgerTor.geometry())

        // we'll allow a few meters deviation. Earth is not perfectly spherical
        GeoGeometry.distance(oranienburgerTor, moved.centroid()) shouldBeLessThan 10.0
        moved as Geometry.Polygon
        moved.coordinates?.get(0)!!.forEach {
            // radius of the circle should be similar, it will change a little
            val radius = GeoGeometry.distance(moved.centroid(), it)
            radius shouldBeGreaterThan 19.0
            radius shouldBeLessThan 21.0
        }
    }

    @Test
    fun scaleXShouldScaleCorrectly() {
        val rectangle = rectangle(brandenBurgerGate, 70.0)
        var cs = rectangle.coordinates!!
        GeoGeometry.distance(cs[0][0], cs[0][1]).roundToLong() shouldBe 70
        GeoGeometry.distance(cs[0][1], cs[0][2]).roundToLong() shouldBe 70
        cs = rectangle.scaleX(30.0).coordinates!!
        GeoGeometry.distance(cs[0][0], cs[0][1]).roundToLong() shouldBe (70.0 * 0.3).toLong()
        cs = rectangle.scaleX(130.0).coordinates!!
        GeoGeometry.distance(cs[0][0], cs[0][1]).roundToLong() shouldBe (70.0 * 1.3).toLong()
        cs = rectangle.scaleY(30.0).coordinates!!
        GeoGeometry.distance(cs[0][1], cs[0][2]).roundToLong() shouldBe (70.0 * 0.3).toLong()
        cs = rectangle.scaleY(130.0).coordinates!!
        GeoGeometry.distance(cs[0][1], cs[0][2]).roundToLong() shouldBe (70.0 * 1.3).toLong()
    }

    @Test
    fun shouldRotateCorrectly() {
        val rectangle = rectangle(brandenBurgerGate, 70.0)
        val centroid = rectangle.centroid()
        var cs = rectangle.coordinates!!
        cs[0].forEach { it.distanceTo(centroid).roundToLong() shouldBe 49 }
        cs = rectangle.rotate(45.0).coordinates!!
        cs[0].forEach { it.distanceTo(centroid).roundToLong() shouldBe 49 }
        cs = rectangle.rotate(360.0).coordinates!!
        cs[0].forEach { it.distanceTo(centroid).roundToLong() shouldBe 49 }
        cs = rectangle.rotate(2000.0).coordinates!!
        cs[0].forEach { it.distanceTo(centroid).roundToLong() shouldBe 49 }
    }

    @Test
    fun shouldScaleAndRotateRectangle() {
        val rectangle = rectangle(brandenBurgerGate, 70.0)

        FeatureCollection(
            listOf(
                rectangle.asFeature(properties = mapOf("fill" to "red").toJsonObject()),
                Geometry.Point(rectangle.centroid())
                    .asFeature(properties = mapOf("marker-color" to "red").toJsonObject()),
                Geometry.Point(rectangle.coordinates!![0][0])
                    .asFeature(properties = mapOf("marker-color" to "blue").toJsonObject()),
                Geometry.Point(rectangle.coordinates!![0][1])
                    .asFeature(properties = mapOf("marker-color" to "yellow").toJsonObject()),
                Geometry.Point(rectangle.coordinates!![0][2])
                    .asFeature(properties = mapOf("marker-color" to "green").toJsonObject()),
                Geometry.Point(rectangle.coordinates!![0][3])
                    .asFeature(properties = mapOf("marker-color" to "purple").toJsonObject()),

                rectangle.scaleX(50.0).asFeature(properties = mapOf("fill" to "blue").toJsonObject()),
                rectangle.scaleY(50.0).asFeature(properties = mapOf("fill" to "purple").toJsonObject()),
                rectangle.scaleX(130.0).asFeature(properties = mapOf("fill" to "green").toJsonObject()),
                rectangle.scaleY(130.0).asFeature(properties = mapOf("fill" to "yellow").toJsonObject()),
                rectangle.rotate(10.0).asFeature(properties = mapOf("fill" to "brown").toJsonObject()),
                rectangle.rotate(45.0).asFeature(properties = mapOf("fill" to "grey").toJsonObject()),

                )
        ).let {
            println(Json.Default.encodeToString(it))
        }
    }

    @Test
    fun shouldMoveInRightDirection() {

        val circle = GeoGeometry.circle2polygon(20, rosenthalerPlatz.latitude, rosenthalerPlatz.longitude, 20.0)
            .polygonGeometry()
        listOf(
            GeoGeometry.translate(circle.centroid().latitude, circle.centroid().longitude, 0.0, 50.0),
            GeoGeometry.translate(circle.centroid().latitude, circle.centroid().longitude, 50.0, 0.0),
            GeoGeometry.translate(circle.centroid().latitude, circle.centroid().longitude, -50.0, 0.0),
            GeoGeometry.translate(circle.centroid().latitude, circle.centroid().longitude, 0.0, -50.0),
        ).forEach { point ->
            GeoGeometry.distance(circle.translate(Geometry.Point(point)).centroid(), point) shouldBeLessThan 1.0
        }
    }

    @Test
    fun shouldRotate() {
        val anchor = bergstr16Berlin
        val point = oranienburgerTor
        val d = GeoGeometry.distance(anchor, point)
        val points = (0..240).step(10).map {
            GeoGeometry.rotateAround(anchor, point, it.toDouble())
        }
            .also {
                // all points should be the same distance
                it.forEach { (GeoGeometry.distance(bergstr16Berlin, it) - d).absoluteValue shouldBeLessThan 0.1 }
                it.size shouldBe 25
                it.distinct().size shouldBe 25
                it.contains(bergstr16Berlin) shouldBe false
            }

        val features = (points + point + anchor).map {
            Geometry.Point(coordinates = it)
        }.map {
            it.asFeature()
        }
        println(FeatureCollection(features))
    }

    @Test
    fun rotateByZeroDegreesShouldBeSamePoint() {
        val anchor = bergstr16Berlin
        val point = oranienburgerTor
        GeoGeometry.distance(point, GeoGeometry.rotateAround(anchor, point, 0.0)) shouldBeLessThan 1.0
        (GeoGeometry.distance(point, GeoGeometry.rotateAround(anchor, point, 180.0)) - 2 * GeoGeometry.distance(
            anchor,
            point
        )).absoluteValue shouldBeLessThan 1.0
    }

    @Test
    fun shouldTranslateCorrectly() {
        val translated = GeoGeometry.translate(52.530564, 13.394964, 1000.0, 3000.0)
        val pythagorasDistance = sqrt(1000.0.pow(2.0) + 3000.0.pow(2.0))
        val distance = GeoGeometry.distance(doubleArrayOf(13.394964, 52.530564), translated)
        withClue("distance should be correct for translated coordinate") {
            abs(distance - pythagorasDistance) shouldBeLessThan 1.0
        }
    }

    @Test
    fun shouldPreserveDimensionsWithMultipleRotations() {
        val triangle = arrayOf(arrayOf(rosenthalerPlatz, moritzPlatz, potsDammerPlatz, rosenthalerPlatz))
        val originalArea = GeoGeometry.area(triangle.polygonGeometry().coordinates!!)
        val triangle15 = triangle.rotate(15.0)
        val triangle18 = triangle15.rotate(3.0)

        val fc = FeatureCollection(listOf(triangle, triangle15, triangle18).map { it.polygonGeometry().asFeature() })

//        println(fc.geoJsonIOUrl)
//        println(DEFAULT_JSON_PRETTY.encodeToString(fc))

        println(triangle.angles)
//        println(triangle15.angles)
//        println(triangle18.angles)


//        abs(GeoGeometry.area(triangle18.rotate(3.0).polygonGeometry().coordinates!!) - originalArea) shouldBeLessThan 1.0
    }

    @Test
    fun shouldCalculateAngleCorrectly() {
        val p1 = doubleArrayOf(13.401361, 52.529948)
        val p2 = doubleArrayOf(13.410717, 52.503663)
        val p3 = doubleArrayOf(13.376599, 52.509515)

        val s1 = arrayOf(p1, p2)
        val s2 = arrayOf(p2, p3)
        val s3 = arrayOf(p3, p1)

//        val a1 = calculateHeadingDifference(s1, s2)
//        println(a1) //119.32
//        val a2 = calculateHeadingDifference(s2, s3)
//        println(a2) //130.73
//        val a3 = calculateHeadingDifference(s3, s1)
//        println(a3) // 109.93
//
//        (a1+a2+a3).roundToInt() shouldBe 180 // actual 360

        val triangle = arrayOf(arrayOf(p1, p2, p3, p1))

//        val around = triangle.centroid()
//        FeatureCollection(
//            listOf(Geometry.Point(around).asFeature())+
//            listOf(triangle, triangle.rotate(45.0,around), triangle.rotate(45.0,around).rotate(45.0,around)).map {it.polygonGeometry().asFeature()}).geoJsonIOUrl.let {
//            println(it)
//        }

        val around = potsDammerPlatz
        val distance = GeoGeometry.distance(around, rosenthalerPlatz)

        val rotatedPoints = (0..36).map { d ->
            rosenthalerPlatz.rotate(d.toDouble() * 10, around)
        }
        rotatedPoints.forEach {
            val actualDistance = GeoGeometry.distance(around, it)
            withClue("$actualDistance is too different from expected $distance") {
                abs(actualDistance - distance) shouldBeLessThan 1.0
            }
        }

        println((rotatedPoints + around).asFeatureCollection(buildJsonObject {

        }).geoJsonIOUrl)
    }
}

val PolygonCoordinates.angles
    get() = outerSegments.let {
        println("segments: ${it.size}")
        it.indices.map { index ->
            println(index)
            calculateAngle(it[index], it[(index + 1) % it.size])
        }
    }


fun rectangle(point: PointCoordinates, size: Double): Geometry.Polygon {
    return arrayOf(
        arrayOf(
            point.translate(size / 2.0, size / -2.0),
            point.translate(size / 2.0, size / 2.0),
            point.translate(size / -2.0, size / 2.0),
            point.translate(size / -2.0, size / -2.0),
            point.translate(size / 2.0, size / -2.0),
        )
    ).polygonGeometry()
}

