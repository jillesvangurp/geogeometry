package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.calculateConcaveHull
import com.jillesvangurp.geojson.Feature
import com.jillesvangurp.geojson.FeatureCollection
import com.jillesvangurp.geojson.Geometry
import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.asFeature
import com.jillesvangurp.geojson.asFeatureWithProperties
import com.jillesvangurp.geojson.geoJsonIOUrl
import com.jillesvangurp.serializationext.DEFAULT_PRETTY_JSON
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan as shouldBeGreaterThanInt
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertFalse

class ConcaveHullFixtureTest {

    private fun List<PointCoordinates>.closedRing(): Array<PointCoordinates> {
        if (this.isEmpty()) return emptyArray()
        val ring = if (first() contentEquals last()) this else this + first()
        return ring.toTypedArray()
    }

    private fun isSelfIntersecting(ring: Array<PointCoordinates>): Boolean {
        if (ring.size < 4) return false
        for (i in 0 until ring.size - 1) {
            val a1 = ring[i]
            val a2 = ring[i + 1]
            for (j in i + 1 until ring.size - 1) {
                // skip adjacent edges and shared vertices
                if (kotlin.math.abs(i - j) <= 1) continue
                // also skip first and last which are the same vertex
                if (i == 0 && j == ring.size - 2) continue
                val b1 = ring[j]
                val b2 = ring[j + 1]
                if (GeoGeometry.linesCross(a1, a2, b1, b2)) {
                    return true
                }
            }
        }
        return false
    }

    private fun logFixtureGeoJson(
        fixture: PolygonPointCloudFixture,
        convex: Array<PointCoordinates>,
        concave: Array<PointCoordinates>
    ) {
        val convexFeature = Geometry.Polygon(arrayOf(convex)).asFeatureWithProperties {
            put("title", JsonPrimitive("Convex ${fixture.name}"))
            put("description", JsonPrimitive(fixture.description))
        }
        val concaveFeature = Geometry.Polygon(arrayOf(concave)).asFeatureWithProperties {
            put("title", JsonPrimitive("Concave ${fixture.name}"))
            put("description", JsonPrimitive(fixture.description))
        }
        val pointFeatures: List<Feature> = fixture.points.mapIndexed { idx, point ->
            Geometry.Point(point).asFeature(
                buildJsonObject {
                    put("title", JsonPrimitive("${fixture.name} #$idx"))
                }
            )
        }

        val featureCollection = FeatureCollection(pointFeatures + convexFeature + concaveFeature)
        println(
            """
            ## ${fixture.name}
            ${fixture.description}
            Convex area: ${GeoGeometry.area(convex)}
            Concave area: ${GeoGeometry.area(concave)}
            GeoJSON: ${featureCollection.geoJsonIOUrl}
            Points: ${
                FeatureCollection(pointFeatures).geoJsonIOUrl
            }
            """.trimIndent()
        )
        println(DEFAULT_PRETTY_JSON.encodeToString(FeatureCollection.serializer(), featureCollection))
    }

    @Test
    fun convexHullFixturesAreValid() {
        concaveHullFixtures.forEach { fixture ->
            val convexRing = GeoGeometry.polygonForPoints(fixture.points.toTypedArray())
            val concaveRing = calculateConcaveHull(fixture.points, fixture.k).closedRing()
            logFixtureGeoJson(fixture, convexRing, concaveRing)

            convexRing.size.shouldBeGreaterThanInt(3)
            assertFalse(isSelfIntersecting(convexRing), "Convex ring self-intersects for ${fixture.name}")
            GeoGeometry.area(convexRing).shouldBeGreaterThan(0.0)
        }
    }

    @Test
    fun concaveHullFixturesAreConcaveAndSmallerThanConvex() {
        concaveHullFixtures.forEach { fixture ->
            val convexRing = GeoGeometry.polygonForPoints(fixture.points.toTypedArray())
            val concaveRing = calculateConcaveHull(fixture.points, fixture.k).closedRing()

            logFixtureGeoJson(fixture, convexRing, concaveRing)

            val convexArea = GeoGeometry.area(convexRing)
            val concaveArea = GeoGeometry.area(concaveRing)

            assertFalse(isSelfIntersecting(concaveRing), "Concave ring self-intersects for ${fixture.name}")
            concaveArea.shouldBeGreaterThan(0.0)
            concaveArea.shouldBeLessThan(convexArea)
        }
    }
}
