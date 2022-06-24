package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geogeometry.oranienburgerTor
import com.jillesvangurp.geogeometry.rosenthalerPlatz
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlin.test.Test

class GeoJsonExtensionsTest {

    @Test
    fun shouldTranslateCircle() {
        val circle = GeoGeometry.circle2polygon(20, rosenthalerPlatz.latitude, rosenthalerPlatz.longitude, 20.0)
            .polygonGeometry()

        val moved = circle.translate(oranienburgerTor.geometry())

        // we'll allow a few meters deviation. Earth is not perfectly spherical
        GeoGeometry.distance(oranienburgerTor, moved.centroid()) shouldBeLessThan 10.0
        moved as Geometry.Polygon
        moved.coordinates?.get(0)!!.forEach {
            // radius of the circle should be similar, it will change a little
            val radius = GeoGeometry.distance(moved.centroid(), it)
            radius shouldBeGreaterThan  19.0
            radius shouldBeLessThan 21.0
        }
    }}