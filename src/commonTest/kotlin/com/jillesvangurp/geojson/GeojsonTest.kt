package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geogeometry.bergstr16Berlin
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GeojsonKtTest {

    @Test
    fun shouldCalculateZoomLevel() {
        val meters = 75.0

        val zl1 = GeoGeometry.bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            meters,
            meters
        ).zoomLevel()

        val zl2 = GeoGeometry.bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            meters * 16,
            meters * 16
        ).zoomLevel()

        println(zl1)
        println(zl2)
        // 2  ^ 4  = 16 x the size means 4 zoom levels difference
        zl1 - zl2 shouldBe 4
    }

    @Test
    fun shouldTile() {

        val bbox = GeoGeometry.bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            100.0 * 16,
            100.0 * 16
        )
        val cells = GeoGeometry.calculateTileBboxesForBoundingBox(bbox)
        println(FeatureCollection(cells.map { it.polygon() }.map { it.asFeature() } + listOf(bbox.polygon().asFeature())))
    }
}
