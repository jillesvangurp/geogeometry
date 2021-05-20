package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geogeometry.json
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldInclude
import org.junit.Test

class GeoJsonJvmTest {
    val berlinJson by lazy { this.javaClass.classLoader.getResource("berlin.geojson")!!.readText() }

    @Test
    fun `cover berlin with hashes and produce valid feature collection json`() {
        val berlin = json.decodeFromString(Geometry.serializer(), berlinJson) as Geometry.MultiPolygon

        val hashes = GeoHashUtils.geoHashesForPolygon(
            berlin.coordinates ?: throw IllegalArgumentException("coordinates missing")
        )
        val hashesCollection = FeatureCollection.fromGeoHashes(hashes)
        val json = json.encodeToString(
            FeatureCollection.serializer(),
            hashesCollection + FeatureCollection(listOf(berlin.asFeature()))
        )
        // make sure we serialize these fields; they are required.
        json shouldInclude """"type":"Feature""""
        json shouldInclude """"type":"FeatureCollection""""
        json shouldInclude """"properties":null"""
        // you can copy paste this to geojson.io
        println(
            json
        )
    }

    @Test
    fun `equals test for geometry`() {
        val berlin = json.decodeFromString(Geometry.serializer(), berlinJson) as Geometry.MultiPolygon

        berlin.shouldBe(berlin)
        berlin.shouldBe(berlin.copy())
        berlin shouldNotBe berlin.copy(coordinates = null)
        berlin.copy(coordinates = null) shouldNotBe berlin
        val reversed = berlin.coordinates?.clone()
        reversed?.reverse()
        berlin.copy(coordinates = reversed ) shouldNotBe berlin

        Geometry.Point(null) shouldBe Geometry.Point(null)
        Geometry.Point(doubleArrayOf(0.1, 0.1)) shouldNotBe Geometry.Point(null)
        Geometry.Point(null) shouldNotBe Geometry.Point(doubleArrayOf(0.1, 0.1))
        Geometry.Point(doubleArrayOf(0.1, 0.1)) shouldNotBe Geometry.Point(doubleArrayOf(0.9, 0.9))
        Geometry.Point(doubleArrayOf(0.1, 0.1)) shouldBe Geometry.Point(doubleArrayOf(0.1, 0.1))
    }
}
