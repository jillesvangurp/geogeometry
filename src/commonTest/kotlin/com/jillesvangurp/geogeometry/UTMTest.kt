package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.format
import com.jillesvangurp.geo.toUTM
import com.jillesvangurp.geo.utmAsWgs84
import com.jillesvangurp.geojson.PointCoordinates
import io.kotest.assertions.assertionCounter
import io.kotest.assertions.collectOrThrow
import io.kotest.assertions.eq.eq
import io.kotest.assertions.errorCollector
import io.kotest.matchers.Matcher
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UTMTest {

    @Test
    fun shouldConvertCoordinates() {
        brandenBurgerGate.toUTM().toString() shouldBe "33 U 389880.94 5819700.4"
        brandenBurgerGate.format shouldBe "52.516279N 13.377157E"
        "33 U 389880.94 5819700.4".utmAsWgs84 shouldBeNear  brandenBurgerGate
    }
}



//@Suppress("UNCHECKED_CAST")
//infix fun <T, U : T> T.shouldBe(expected: U?): T {
//    when (expected) {
//        is Matcher<*> -> should(expected as Matcher<T>)
//        else -> {
//            val actual = this
//            assertionCounter.inc()
//            eq(actual, expected)?.let(errorCollector::collectOrThrow)
//        }
//    }
//    return this
//}