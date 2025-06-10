package com.jillesvangurp.geogeometry

import com.jillesvangurp.geogeometry.geometry.*
import com.jillesvangurp.geojson.*
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.*
import kotlin.test.Test
import com.jillesvangurp.geojson.lonLat

class RotationTest {

    @Test
    fun shouldTranslateCircle() {
        val circle = circle2polygon(20, rosenthalerPlatz.latitude, rosenthalerPlatz.longitude, 20.0)
            .polygonGeometry()

        val moved = circle.translate(oranienburgerTor.geometry())

        // we'll allow a few meters deviation. Earth is not perfectly spherical
        distance(oranienburgerTor, moved.centroid()) shouldBeLessThan 10.0
        moved as Geometry.Polygon
        moved.outerCoordinates.forEach {
            // radius of the circle should be similar, it will change a little
            val radius = distance(moved.centroid(), it)
            radius shouldBeGreaterThan 19.0
            radius shouldBeLessThan 21.0
        }
    }

    @Test
    fun scaleXShouldScaleCorrectly() {
        val rectangle = rectangle(brandenBurgerGate, 70.0)
        var cs = rectangle.coordinates!!
        distance(cs[0][0], cs[0][1]).roundToLong() shouldBe 70
        distance(cs[0][1], cs[0][2]).roundToLong() shouldBe 70
        cs = rectangle.scaleX(30.0).coordinates!!
        distance(cs[0][0], cs[0][1]).roundToLong() shouldBe (70.0 * 0.3).toLong()
        cs = rectangle.scaleX(130.0).coordinates!!
        distance(cs[0][0], cs[0][1]).roundToLong() shouldBe (70.0 * 1.3).toLong()
        cs = rectangle.scaleY(30.0).coordinates!!
        distance(cs[0][1], cs[0][2]).roundToLong() shouldBe (70.0 * 0.3).toLong()
        cs = rectangle.scaleY(130.0).coordinates!!
        distance(cs[0][1], cs[0][2]).roundToLong() shouldBe (70.0 * 1.3).toLong()
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

        val circle = circle2polygon(20, rosenthalerPlatz.latitude, rosenthalerPlatz.longitude, 20.0)
            .polygonGeometry()
        listOf(
            translate(circle.centroid().latitude, circle.centroid().longitude, 0.0, 50.0),
            translate(circle.centroid().latitude, circle.centroid().longitude, 50.0, 0.0),
            translate(circle.centroid().latitude, circle.centroid().longitude, -50.0, 0.0),
            translate(circle.centroid().latitude, circle.centroid().longitude, 0.0, -50.0),
        ).forEach { point ->
            distance(circle.translate(Geometry.Point(point)).centroid(), point) shouldBeLessThan 1.0
        }
    }

    @Test
    fun shouldRotate() {
        val anchor = bergstr16Berlin
        val point = oranienburgerTor
        val d = distance(anchor, point)
        val points = (0..240).step(10).map {
            rotateAround(anchor, point, it.toDouble())
        }
            .also {
                // all points should be the same distance
                it.forEach { (distance(bergstr16Berlin, it) - d).absoluteValue shouldBeLessThan 0.1 }
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
        distance(point, rotateAround(anchor, point, 0.0)) shouldBeLessThan 1.0
        (distance(point, rotateAround(anchor, point, 180.0)) - 2 * distance(
            anchor,
            point
        )).absoluteValue shouldBeLessThan 1.0
    }

    @Test
    fun shouldTranslateCorrectly() {
        val translated = translate(52.530564, 13.394964, 1000.0, 3000.0)
        val pythagorasDistance = sqrt(1000.0.pow(2.0) + 3000.0.pow(2.0))
        val distance = distance(lonLat(13.394964, 52.530564), translated)
        withClue("distance should be correct for translated coordinate") {
            abs(distance - pythagorasDistance) shouldBeLessThan 1.0
        }
    }

    @Test
    fun shouldRotatePointAround() {
        val around = moritzPlatz
        val distance = distance(around, rosenthalerPlatz)

        val rotatedPoints = (0..36).map { d ->
            rosenthalerPlatz.rotate(d.toDouble() * 10, around).geometry().asFeatureWithProperties {
                val gray = (d.toDouble()/36*255).roundToInt().toString(16).padStart(2,'0')
                markerColor("#$gray$gray$gray")
                title("$d")
            }
        }
        rotatedPoints.forEach {
            val actualDistance = distance(around, (it.geometry as Geometry.Point).coordinates!!)
            withClue("$actualDistance is too different from expected $distance") {
                abs(actualDistance - distance) shouldBeLessThan 1.0
            }
        }

        println(FeatureCollection(rotatedPoints + around.geometry().asFeatureWithProperties {
            markerColor("red")
        }).geoJsonIOUrl)
    }

    @Test
    fun shouldRotateTriangle() {
        val triangle = arrayOf(arrayOf(rosenthalerPlatz, moritzPlatz, potsDammerPlatz, rosenthalerPlatz))
        FeatureCollection(listOf(
        triangle.polygonGeometry().asFeatureWithProperties {
            fill("red")
            fillOpacity(0.25)
        },
        rosenthalerPlatz.geometry().asFeatureWithProperties {
            markerColor("green")
            title("og rosenthaler")
        },
        rosenthalerPlatz.rotate(0.0, moritzPlatz).geometry().asFeatureWithProperties {
            markerColor("pink")
            title("0 degrees")
        },
        rosenthalerPlatz.rotate(10.0, moritzPlatz).geometry().asFeatureWithProperties {
            markerColor("yellow")
            title("10 degrees")
        },
        potsDammerPlatz.rotate(20.0, rosenthalerPlatz).geometry().asFeatureWithProperties {
            fill("brown")
            fillOpacity(0.25)
            title("20 degrees potsdammerplatz")
        },
        triangle.rotate(20.0, rosenthalerPlatz).polygonGeometry().asFeatureWithProperties {
            fill("green")
            fillOpacity(0.25)
        },
        triangle.rotate(20.0, rosenthalerPlatz).rotate(20.0, rosenthalerPlatz).polygonGeometry().asFeatureWithProperties {
            fill("blue")
            fillOpacity(0.25)
        }
        )).geoJsonIOUrl.let {
            println(it)
        }
    }

    @Test
    fun shouldNotDistortTriangleWhenRotatingManyTimes() {
        val triangle = arrayOf(arrayOf(rosenthalerPlatz, moritzPlatz, potsDammerPlatz, rosenthalerPlatz))
        var rotated = triangle
        repeat(200) {
            // it all gets translated to radians so 5000 degrees is fine
            rotated = rotated.rotate(5000.0, triangle.centroid())
        }
        triangle.outerSegments.zip(rotated.outerSegments).forEach { (originalSegment, rotatedSegment) ->
            // verify segments have same length as the original even after rotating many times
            val ogLength = distance(originalSegment[0], originalSegment[1])
            val rotatedLength = distance(rotatedSegment[0], rotatedSegment[1])
            ogLength - rotatedLength shouldBeLessThan 1.0
        }

        listOf(
            triangle,
            rotated
        ).map { it.polygonGeometry() }.asFeatureCollection().let {
            println(it.geoJsonIOUrl)
        }
    }

    @Test
    fun shouldMoveBackAndForth() {
        val moved = rosenthalerPlatz.translate(-1000.0,-2000.0).translate(1000.0,2000.0)
        distance(rosenthalerPlatz,moved) shouldBeLessThan 1.0
    }


    @Test
    fun shouldNotDistortRotatedRectanglesWhenScaling() {
        val rectangle = rectangle(rosenthalerPlatz,4.0).rotate(30.0)
        var scaled = rectangle
        (1..5).forEach { percent ->
            scaled = scaled.scaleX(percent*102.0).scaleY(percent*102.0)
        }


        println(FeatureCollection(
            listOf(rectangle, scaled).map { it.asFeature() }
        ).geoJsonIOUrl)
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

