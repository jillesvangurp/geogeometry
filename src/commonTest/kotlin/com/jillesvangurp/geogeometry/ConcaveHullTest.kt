package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.calculateConcaveHull
import com.jillesvangurp.geojson.FeatureCollection
import com.jillesvangurp.geojson.Geometry
import com.jillesvangurp.geojson.asFeature
import kotlinx.serialization.json.Json
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
        val polygon = Json.decodeFromString(Geometry.Polygon.serializer(), concavePoly)

        val p = Geometry.Polygon(coordinates = arrayOf(calculateConcaveHull(polygon.coordinates!![0].toList().shuffled(), 7).toTypedArray()))

        println(Json {
            prettyPrint = true
        }.encodeToString(FeatureCollection.serializer(), FeatureCollection(listOf(p.asFeature(), polygon.asFeature()))))

    }
}