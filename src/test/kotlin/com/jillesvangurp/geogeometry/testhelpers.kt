package com.jillesvangurp.geogeometry

import io.kotlintest.matchers.doubles.shouldBeLessThan
import kotlin.math.abs

private const val defaultMarginOfError = 0.0000001

fun Double.shouldBeApproximately(other: Double, marginOfError: Double = defaultMarginOfError) {
    // allow for tiny rounding errors
    abs(this - other) shouldBeLessThan 0.000001
}

infix fun Double.shouldBeApproximately(other: Double) {
    this.shouldBeApproximately(other, defaultMarginOfError)
}