package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.calculateConcaveHull
import com.jillesvangurp.geojson.*
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test

class ConcaveHullTest {

    @Test
    fun shouldCalculateConcaveHull() {
        val points = listOf(bergstr16Berlin, rosenthalerPlatz, oranienburgerTor, senefelderPlatz, potsDammerPlatz, naturkundeMuseum)

        val p = Geometry.Polygon(coordinates = arrayOf(calculateConcaveHull(points, 4).toTypedArray()))
        println(p.toString())
    }

    @Test
    fun shouldCalculateAppropriatePolygon() {
        val polygon = Json.decodeFromString(Geometry.serializer(), concavePoly) as Geometry.Polygon

        val p = Geometry.Polygon(coordinates = arrayOf(calculateConcaveHull(
            polygon.coordinates!![0].toList().shuffled(),
            7
        ).toTypedArray()))

        println(
            jsonPretty.encodeToString(FeatureCollection.serializer(), FeatureCollection(listOf(p.asFeature(), polygon.asFeature())))
        )
    }

    @Test
    fun shouldCalculatePolygonForRandomPoints() {

        val points = (0..50).map {
            val x = Random.nextInt(0, 500)
            val y = Random.nextInt(0, 500)
            bergstr16Berlin.translate(y.toDouble(), x.toDouble())
        }

        val hullCoords = calculateConcaveHull(points, 7)

        val hull = arrayOf(hullCoords.toTypedArray()).polygonGeometry().asFeature()

        (points.map { Geometry.Point(it).asFeature() }  + hull).let {
            println(FeatureCollection(it))
        }
    }
}
