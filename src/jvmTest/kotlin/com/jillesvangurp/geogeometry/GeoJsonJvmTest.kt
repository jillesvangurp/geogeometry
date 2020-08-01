package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.FeatureCollection
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geo.MultiPolygonGeometry
import org.junit.Test

class GeoJsonJvmTest {
    @Test
    fun `cover berlin with hashes`() {
        val berlinJson = this.javaClass.classLoader.getResource("berlin.geojson").readText()
        val berlin = gson.fromJson(berlinJson, MultiPolygonGeometry::class.java)

        val hashes = GeoHashUtils.geoHashesForPolygon(
            berlin.coordinates ?: throw IllegalArgumentException("coordinates missing")
        )
        val hashesCollection = FeatureCollection.fromGeoHashes(hashes)
        println(gson.toJson(hashesCollection + FeatureCollection(listOf(berlin.asFeature()))))
    }
}
