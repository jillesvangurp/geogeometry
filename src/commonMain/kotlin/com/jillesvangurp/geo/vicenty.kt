/**
 * Adapted from https://github.com/grumlimited/geocalc/blob/v0.5.1/src/main/java/com/grum/geocalc/EarthCalc.java
 *
 * I did some renaming to get rid of kotlin warnings (all the greek symbols) and for clarity
 * and some code re-structuring more appropriate for Kotlin.
 *
 * BSD 3-Clause License
 *
 * Copyright (c) 2018, Grum Ltd (Romain Gallet)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Geocalc nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jillesvangurp.geo

import com.jillesvangurp.geo.GeoGeometry.Companion.fromRadians
import com.jillesvangurp.geo.GeoGeometry.Companion.toRadians
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Outcome of the vicenty algorithm.
 * distance is the distance in meter
 * initialBearing is the initial bearing, or forward azimuth (in reference to North point), in degrees
 * finalBearing is the final bearing (in direction p1→p2), in degrees
 */
data class Vincenty(
    val distance: Double = 0.0,
    val initialBearing: Double = 0.0,
    val finalBearing: Double = 0.0
) {
    companion object {
        val CO_INCIDENT_POINTS = Vincenty(0.0, 0.0, 0.0)
    }
}

private const val EARTH_RADIUS = 6356752.314245 // official radius
private const val RADIUS_AT_EQUATOR: Double = 6378137.0 // radius at equator

/**
 * Calculate distance in meters, initial bearing and final bearing using the Vicenty algorithm.
 *
 * This provides better accuracy than Haversine as it takes into account the flattening of the earth. Note, this
 * is still an approximation of course. Over short distances, there should not be any difference.
 */
private fun vincenty(p1: PointCoordinates, p2: PointCoordinates): Vincenty {
    val lon1Rad: Double = toRadians(p1.longitude)
    val lon2Rad: Double = toRadians(p2.longitude)
    val lat1Rad: Double = toRadians(p1.latitude)
    val lat2rad: Double = toRadians(p2.latitude)
    val flattening: Double = 1 / 298.257223563 // flattening of the ellipsoid
    val lonRadDifference: Double = lon2Rad - lon1Rad
    val tanU1: Double = (1 - flattening) * tan(lat1Rad)
    val cosU1: Double = 1 / sqrt(1 + tanU1 * tanU1)
    val sinU1: Double = tanU1 * cosU1
    val tanU2: Double = (1 - flattening) * tan(lat2rad)
    val cosU2: Double = 1 / sqrt(1 + tanU2 * tanU2)
    val sinU2: Double = tanU2 * cosU2

    var lambda: Double = lonRadDifference
    var lambdaPrevious: Double
    var iterationLimit = 100.0
    var cosSqAlpha: Double
    var sigma: Double
    var cos2SigmaM: Double
    var cosSigma: Double
    var sinSigma: Double
    var sinLambda: Double
    var cosLambda: Double
    do {
        sinLambda = sin(lambda)
        cosLambda = cos(lambda)
        val sinSqSigma: Double =
            cosU2 * sinLambda * (cosU2 * sinLambda) + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
        sinSigma = sqrt(sinSqSigma)
        if (sinSigma == 0.0) return Vincenty.CO_INCIDENT_POINTS // co-incident points
        cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
        sigma = atan2(sinSigma, cosSigma)
        val sinAlpha: Double = cosU1 * cosU2 * sinLambda / sinSigma
        cosSqAlpha = (1 - sinAlpha * sinAlpha).toDouble()
        cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha

        cos2SigmaM.isNaN()
        if (cos2SigmaM.isNaN()) cos2SigmaM = 0.0 // equatorial line: cosSqα=0
        val c: Double = flattening / 16 * cosSqAlpha * (4 + flattening * (4 - 3 * cosSqAlpha))
        lambdaPrevious = lambda
        lambda = lonRadDifference + (1 - c) * flattening * sinAlpha * (sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)))
    } while (abs(lambda - lambdaPrevious) > 1e-12 && --iterationLimit > 0)
    check(iterationLimit != 0.0) { "Formula failed to converge" }
    val uSq: Double =
        cosSqAlpha * (RADIUS_AT_EQUATOR * RADIUS_AT_EQUATOR - EARTH_RADIUS // Using b to keep close to academic formula.
                * EARTH_RADIUS // Using b to keep close to academic formula.
                ) / (EARTH_RADIUS // Using b to keep close to academic formula.
                * EARTH_RADIUS // Using b to keep close to academic formula.
                )
    val A: Double = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
    val B: Double = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))
    val deltaSigma: Double = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) -
            B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)))
    val distance: Double = EARTH_RADIUS * A * (sigma - deltaSigma) // Using b to keep close to academic formula.

    var initialBearing = atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
    initialBearing = (initialBearing + 2 * PI) % (2 * PI) //turning value to trigonometric direction
    var finalBearing = atan2(cosU1 * sinLambda, -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda)
    finalBearing = (finalBearing + 2 * PI) % (2 * PI) //turning value to trigonometric direction
    return Vincenty(distance, fromRadians(initialBearing), fromRadians(finalBearing))
}

fun vicentyDistance(p1: PointCoordinates, p2: PointCoordinates) = vincenty(p1, p2).distance
fun vicentyDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) = vicentyDistance(
    doubleArrayOf(lon1, lat1),
    doubleArrayOf(lon2, lat2)
)
