package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry.Companion.changeOrder
import com.jillesvangurp.geo.GeoGeometry.Companion.ensureFollowsRightHandSideRule
import com.jillesvangurp.geo.GeoGeometry.Companion.hasSameStartAndEnd
import com.jillesvangurp.geo.GeoGeometry.Companion.isValid
import com.jillesvangurp.geojson.Geometry
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GeoGeometryTest {
    val bergstr16Berlin = doubleArrayOf(13.3941763, 52.5298311)
    val brandenBurgerGate = doubleArrayOf(13.377157, 52.516279)
    val potsDammerPlatz = doubleArrayOf(13.376599, 52.509515)
    val moritzPlatz = doubleArrayOf(13.410717, 52.503663)
    val senefelderPlatz = doubleArrayOf(13.412949, 52.532755)
    val naturkundeMuseum = doubleArrayOf(13.381921, 52.531188)
    val rosenthalerPlatz = doubleArrayOf(13.401361, 52.529948)
    val oranienburgerTor = doubleArrayOf(13.38707, 52.525339)

    @Test
    fun shouldBeValidPolygon() {
        val bigRing = arrayOf(potsDammerPlatz,brandenBurgerGate,naturkundeMuseum,senefelderPlatz,moritzPlatz,potsDammerPlatz)
        val smallRing = arrayOf(rosenthalerPlatz,oranienburgerTor,bergstr16Berlin,rosenthalerPlatz)
        bigRing.hasSameStartAndEnd() shouldBe true
        smallRing.hasSameStartAndEnd() shouldBe true

        arrayOf(bigRing).isValid() shouldBe false
        arrayOf(bigRing.changeOrder()).isValid() shouldBe true

        arrayOf(smallRing).isValid() shouldBe false
        arrayOf(smallRing.changeOrder()).isValid() shouldBe true

        arrayOf(bigRing, smallRing.changeOrder()).also { println(Geometry.Polygon(it)) }.isValid() shouldBe false
        arrayOf(bigRing.changeOrder(), smallRing).also {
            println(Geometry.Polygon(it))
        }.isValid() shouldBe true
        arrayOf(bigRing.changeOrder(), smallRing.changeOrder()).isValid() shouldBe false

        arrayOf(bigRing, smallRing.changeOrder()).ensureFollowsRightHandSideRule().isValid() shouldBe true
        arrayOf(bigRing.changeOrder(), smallRing).ensureFollowsRightHandSideRule().isValid() shouldBe true
        arrayOf(bigRing.changeOrder(), smallRing.changeOrder()).ensureFollowsRightHandSideRule().isValid() shouldBe true
    }
}