package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.GeoGeometry.Companion.roundDecimals
import com.jillesvangurp.geojson.FeatureCollection
import com.jillesvangurp.geojson.Geometry
import com.jillesvangurp.geojson.asFeature
import com.jillesvangurp.geojson.geoJsonIOUrl
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.test.Test
import kotlinx.serialization.json.JsonPrimitive
import com.jillesvangurp.geojson.lonLat
import com.jillesvangurp.geojson.latLon
import kotlinx.serialization.json.buildJsonObject


enum class MapProviders {
    Here, Google, Osm, Apple
}

fun DoubleArray.roundAndReverse() = this.map { it.roundDecimals(8) }.toDoubleArray().also {it.reverse()}

class MapAlignmentTest {

    val referencePoints = mapOf(
        "Ahoy Berlin NW Building Corner in Berlin" to mapOf(
            MapProviders.Osm to latLon(52.54150, 13.38982),
            MapProviders.Google to latLon(52.541510535244676, 13.38980753280997),
//            MapProviders.Here to doubleArrayOf(52.54131,13.38965).roundAndReverse(),
            MapProviders.Here to latLon(52.54150630324809, 13.389818199084072),
            MapProviders.Apple to latLon(52.541497, 13.389822)
        ),
        "Siege Säule Centroid" to mapOf(
            MapProviders.Osm to latLon(52.51450, 13.35012),
            MapProviders.Google to latLon(52.51451224763399, 13.35010044553336),
            MapProviders.Here to latLon(52.514514513886496, 13.35009726850834),
            MapProviders.Apple to latLon(52.514524, 13.350096),
        ),
        "Galeria Kaufhof SW corner" to mapOf(
            MapProviders.Osm to latLon(52.52185, 13.41171),
            MapProviders.Google to latLon(52.52184966204656, 13.411722336815041),
            MapProviders.Here to latLon(52.52184025422203, 13.411664047844162),
            MapProviders.Apple to latLon(52.521853, 13.411718),
        )
    )

    @Test
    fun distanceForGoogleHereAndOsmForReferencePoints() {
        referencePoints.forEach { (testCase, points) ->
            println(
                """
                ### $testCase
                
                | Provider 1 | Point | Provider 2 | Point | Distance (meters) |
                |----|----|----|----|----|
            """.trimIndent()
            )


            val ps = points.toList()
            val seen = mutableSetOf<List<MapProviders>>()

            ps.indices.forEach { index ->
                val (refProvider, refPoint) = ps[index]
                ps.indices.forEach { i ->
                    if (i != index) {
                        val (otherProvider, otherPoint) = ps[i]
                        val providerPair = listOf(refProvider, otherProvider).sorted()
                        if (!seen.contains(providerPair)) {
                            println(
                                "| $refProvider | ${refPoint.latitude}, ${refPoint.longitude} | $otherProvider | ${otherPoint.latitude}, ${otherPoint.longitude} | ${
                                    GeoGeometry.distance(
                                        refPoint,
                                        otherPoint
                                    ).roundDecimals(2)
                                } |"
                            )
                            seen.add(providerPair)
                        }
                    }
                }
            }
            val url = ps.map { (provider, point) ->
                Geometry.Point(point).asFeature(
                    buildJsonObject {
                        put("title", JsonPrimitive(provider.name))
                    }
                )
            }.let {
                FeatureCollection(it).geoJsonIOUrl
            }
            println("""
                
                [Review on geojson.io]($url)
                
            """.trimIndent())
        }
    }
}