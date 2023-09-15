@file:OptIn(ExperimentalSerializationApi::class)

package com.jillesvangurp.geogeometry

import com.jillesvangurp.geojson.PointCoordinates
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.math.abs

infix fun PointCoordinates.shouldBeNear(expected: PointCoordinates) {
    val distance = com.jillesvangurp.geo.GeoGeometry.distance(this,expected)
    distance shouldBeLessThan 1.0
}

fun Double.shouldBeApproximately(other: Double, marginOfError: Double = 0.0000001) {
    // allow for tiny rounding errors
    abs(this - other) shouldBeLessThan marginOfError
}

infix fun Double.shouldBeApproximately(other: Double) {
    this.shouldBeApproximately(other, 0.0000001)
}

val json: Json by lazy {
    Json {
        // don't rely on external systems being written in kotlin or even having a language with default values
        // the default of false is FFing insane and dangerous
        encodeDefaults = true
        // save space
        prettyPrint = false
        // people adding shit to the json is OK, we're forward compatible and will just ignore it
        isLenient = true
        // encoding nulls is meaningless and a waste of space.
        explicitNulls = false
        // adding enum values is OK even if older clients won't understand it
        ignoreUnknownKeys = true
    }
}


val jsonPretty: Json by lazy {
    Json {
        // don't rely on external systems being written in kotlin or even having a language with default values
        // the default of false is FFing insane and dangerous
        encodeDefaults = true
        // save space
        prettyPrint = false
        // people adding shit to the json is OK, we're forward compatible and will just ignore it
        isLenient = true
        // encoding nulls is meaningless and a waste of space.
        explicitNulls = false
        // adding enum values is OK even if older clients won't understand it
        ignoreUnknownKeys = true
        prettyPrint = true
    }
}
