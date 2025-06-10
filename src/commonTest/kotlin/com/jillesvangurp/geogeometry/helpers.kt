@file:OptIn(ExperimentalSerializationApi::class)

package com.jillesvangurp.geogeometry

import com.jillesvangurp.geogeometry.core.PointCoordinates
import com.jillesvangurp.serializationext.DEFAULT_JSON
import io.kotest.matchers.doubles.shouldBeLessThan
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.math.abs

import com.jillesvangurp.geogeometry.geometry.*

infix fun PointCoordinates.shouldBeNear(expected: PointCoordinates) {
    val distance = distance(this,expected)
    distance shouldBeLessThan 1.0
}

fun Double.shouldBeApproximately(other: Double, marginOfError: Double = 0.0000001) {
    // allow for tiny rounding errors
    abs(this - other) shouldBeLessThan marginOfError
}

infix fun Double.shouldBeApproximately(other: Double) {
    this.shouldBeApproximately(other, 0.0000001)
}

fun Map<String,String>.toJsonObject() = DEFAULT_JSON.encodeToJsonElement(this).jsonObject

