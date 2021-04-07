package com.jillesvangurp.geogeometry

import io.kotest.matchers.doubles.shouldBeLessThan
import kotlinx.serialization.json.Json
import kotlin.math.abs

fun Double.shouldBeApproximately(other: Double, marginOfError: Double = 0.0000001) {
    // allow for tiny rounding errors
    abs(this - other) shouldBeLessThan marginOfError
}

infix fun Double.shouldBeApproximately(other: Double) {
    this.shouldBeApproximately(other, 0.0000001)
}

val json: Json = Json { }