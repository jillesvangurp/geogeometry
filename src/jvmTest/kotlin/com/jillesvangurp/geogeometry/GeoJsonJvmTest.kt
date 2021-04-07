package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geojson.FeatureCollection
import com.jillesvangurp.geojson.Geometry
import org.junit.Test

class GeoJsonJvmTest {
    @Test
    fun `cover berlin with hashes`() {
        val berlinJson = this.javaClass.classLoader.getResource("berlin.geojson").readText()
        val berlin = json.decodeFromString(Geometry.serializer(), berlinJson) as Geometry.MultiPolygonGeometry

        val hashes = GeoHashUtils.geoHashesForPolygon(
            berlin.coordinates ?: throw IllegalArgumentException("coordinates missing")
        )
        val hashesCollection = FeatureCollection.fromGeoHashes(hashes)
        println(
            json.encodeToString(
                FeatureCollection.serializer(),
                hashesCollection + FeatureCollection(listOf(berlin.asFeature()))
            )
        )
    }
}
