package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.FeatureCollection
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geo.Geometry
import kotlinx.serialization.json.Json
import org.junit.Test

class GeoJsonJvmTest {
    val json = Json {}
    @Test
    fun `cover berlin with hashes`() {
        val berlinJson = this.javaClass.classLoader.getResource("berlin.geojson").readText()
        val berlin = json.decodeFromString(Geometry.serializer(), berlinJson) as Geometry.MultiPolygonGeometry

        val hashes = GeoHashUtils.geoHashesForPolygon(
            berlin.coordinates ?: throw IllegalArgumentException("coordinates missing")
        )
        val hashesCollection = FeatureCollection.fromGeoHashes(hashes)
        println(gson.toJson(hashesCollection + FeatureCollection(listOf(berlin.asFeature()))))
    }
}
