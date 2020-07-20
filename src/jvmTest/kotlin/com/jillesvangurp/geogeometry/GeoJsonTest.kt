package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.FeatureCollection
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geo.MultiPolygonCoordinates
import com.jillesvangurp.geo.MultiPolygonGeometry
import com.jillesvangurp.geo.PolygonGeometry
import org.junit.jupiter.api.Test

class GeoJsonTest {
    @Test
    fun `cover berlin with hashes`() {
        val berlinJson = this.javaClass.classLoader.getResource("berlin.geojson").readText()
        val berlin = gson.fromJson(berlinJson, MultiPolygonGeometry::class.java)

        val hashes = GeoHashUtils.geoHashesForPolygon(berlin.coordinates ?: throw IllegalArgumentException("coordinates missing"))
        val hashesCollection = FeatureCollection.fromGeoHashes(hashes)
        println(gson.toJson(hashesCollection + FeatureCollection(listOf(berlin.asFeature()))))
    }
}
