package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GeoGeometryTest : StringSpec() {
    init {
        "check intersection point for both lines when one of the lines is vertical - issue #12" {
            // reported by a user
            // line 1 is vertical
            // line 2 intersects in between the points of line 1
            // however, the intersection point is outside of the points of line2 (this was the bug)
            val l1p1 = doubleArrayOf(-71.1884310511, 42.3219864254)
            val l1p2 = doubleArrayOf(-71.1884310511, 42.321998793)
            val l2p1 = doubleArrayOf(-71.1884310515, 42.3221529806)
            val l2p2 = doubleArrayOf(-71.1884310517, 42.3222331303)

            GeoGeometry.linesCross(l1p1, l1p2, l2p1, l2p2) shouldBe false
            GeoGeometry.linesCross(l2p1, l2p2, l1p1, l1p2) shouldBe false
        }
    }
}
