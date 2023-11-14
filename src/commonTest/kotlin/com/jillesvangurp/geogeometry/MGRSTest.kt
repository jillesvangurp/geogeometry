package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.convertUTMToMGRS
import com.jillesvangurp.geo.toUtmCoordinate
import kotlin.test.Test

class MGRSTest {
    @Test
    fun shouldCalculateMgrsForBrandenburgerTor() {
        val mgrs = brandenBurgerGate.toUtmCoordinate().convertUTMToMGRS()
        println(mgrs)
    }
}