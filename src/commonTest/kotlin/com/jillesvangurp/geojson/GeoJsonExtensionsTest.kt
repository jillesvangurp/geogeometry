package com.jillesvangurp.geojson

import com.jillesvangurp.geogeometry.geometry.*
import com.jillesvangurp.geogeometry.oranienburgerTor
import com.jillesvangurp.geogeometry.rosenthalerPlatz
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

class GeoJsonExtensionsTest {

    @Test
    fun shouldCreateTriangle() {
        circle2polygon(3, rosenthalerPlatz.latitude, rosenthalerPlatz.longitude, 20.0)
            .polygonGeometry().let {
                println(it)
            }
    }


}


