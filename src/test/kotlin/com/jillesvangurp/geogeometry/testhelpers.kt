package com.jillesvangurp.geogeometry

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlin.math.abs

private const val defaultMarginOfError = 0.0000001

fun Double.shouldBeApproximately(other: Double, marginOfError: Double = defaultMarginOfError) {
    // allow for tiny rounding errors
    abs(this - other) shouldBeLessThan 0.000001
}

infix fun Double.shouldBeApproximately(other: Double) {
    this.shouldBeApproximately(other, defaultMarginOfError)
}

// we don't want gson a dependency but useful when debugging since you can serialize feature collections
// normally we only have this as a test dependency
val gsonp: Gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
val gson: Gson = GsonBuilder().serializeNulls().create()
