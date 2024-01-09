package com.jillesvangurp.geo

import com.jillesvangurp.geojson.LineSegment
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

private fun vectorMagnitude(vector: DoubleArray): Double {
    return sqrt(vector[0].pow(2) + vector[1].pow(2))
}

private fun dotProduct(v1: DoubleArray, v2: DoubleArray): Double {
    if (v1.size != v2.size) {
        throw IllegalArgumentException("Arrays must be of the same length")
    }

    return v1.indices.sumOf { v1[it] * v2[it] }
}

private fun crossProduct(v1: DoubleArray, v2: DoubleArray): Double {
    return v1[0] * v2[1] - v1[1] * v2[0]
}

// FIXME experimental
fun calculateAngle(s1: LineSegment, s2: LineSegment): Double {
    val p1 = s1[0]
    val p2 = s1[1]
    val p3 = s2[0]
    val p4 = s2[1]

    val v1 = doubleArrayOf(p2.longitude-p1.longitude, p2.latitude-p1.latitude)
    val v2 = doubleArrayOf(p4.longitude-p3.longitude, p4.latitude-p3.latitude)

    val dot = dotProduct(v1,v2)
    val m1 = vectorMagnitude(v1)
    val m2 = vectorMagnitude(v2)

    val cosineAngle = dot / (m1 * m2)
    val angle = acos(cosineAngle.coerceIn(-1.0, 1.0)) * (180 / PI)

    val cross = crossProduct(v1,v2)
    return if(cross>0) angle else 180-angle
}

fun calculateHeadingDifference(s1: LineSegment, s2: LineSegment): Double {
    val h1 = GeoGeometry.headingFromTwoPoints(s1[0],s1[1])
    val h2 = GeoGeometry.headingFromTwoPoints(s2[0],s2[1])
    println("$h1 $h2 ${h1-h2}")
    return h1-h2
}
