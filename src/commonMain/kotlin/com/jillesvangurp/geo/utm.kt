package com.jillesvangurp.geo

/*
 * Converted to Kotlin and adapted from:
 * https://github.com/tarelli/jscience/blob/master/src/org/jscience/geography/coordinates/UTM.java
 * https://github.com/tarelli/jscience/blob/master/src/org/jscience/geography/coordinates/crs/ReferenceEllipsoid.java
 *
 * JScience - Java(TM) Tools and Libraries for the Advancement of Sciences.
 * Copyright (C) 2006 - JScience (http://jscience.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
import com.jillesvangurp.geogeometry.geometry.*
import com.jillesvangurp.geogeometry.core.PointCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import com.jillesvangurp.geojson.normalize
import kotlin.math.*

/**
 * The UTM scale factor. This the exact scale factor only on a pair of
 * lines lying either side of the central meridian, but the effect is to
 * reduce overall distortion within the UTM zone to less than one part per
 * thousand.
 */
private const val UTM_SCALE_FACTOR = 0.9996

/**
 * The UTM "false easting" value. This quantity is added to the true
 * easting to avoid using negative numbers in the coordinates.
 */
private const val UTM_FALSE_EASTING = 500000.0

/**
 * The UTM "false northing" value. This quantity is added to the true
 * northing for coordinates **in the southern hemisphere only**
 * to avoid using negative numbers in the coordinates.
 */
private const val UTM_FALSE_NORTHING = 10000000.0

/**
 * The northern limit of the UTM grid. Beyond this limit the distortion
 * introduced by the transverse Mercator projection is impractically
 * large, and the UPS grid is used instead.
 */
private const val UTM_NORTHERN_LIMIT = 84.0

/**
 * The southern limit of the UTM grid. Beyond this limit the distortion
 * introduced by the transverse Mercator projection is impractically
 * large, and the UPS grid is used instead.
 */
private const val UTM_SOUTHERN_LIMIT = -80.0

/**
 * The UPS scale factor.
 */
const val UPS_SCALE_FACTOR = 0.994

/**
 * The UPS "false easting" value. This quantity is added to the true
 * easting to avoid using negative numbers in the coordinates.
 */
const val UPS_FALSE_EASTING = 2000000.0


/**
 * The UPS "false northing" value. This quantity is added to the true
 * northing to avoid using negative numbers in the coordinates.
 * The UPS system, unlike the UTM system, always includes the false northing.
 */
const val UPS_FALSE_NORTHING = 2000000.0

/*
* NOTE: The calculations in this class use power series expansions.
* The naming convention is to include the power in the name
* of the term, so that the square of K0 is 'K02', the cube
* is 'K03', etc.
*/
private val K0: Double = UTM_SCALE_FACTOR
private val K02: Double = K0 * K0
private val K03: Double = K02 * K0
private val K04: Double = K03 * K0
private val K05: Double = K04 * K0
private val K06: Double = K05 * K0
private val K07: Double = K06 * K0
private val K08: Double = K07 * K0


/**
 * Class representing UTM or UPS coordinates.
 *
 * Which coordinate system is applicable can be determined from the latitude letter. Use the [isUps]
 * and [isUtm] functions for this. There are separate conversion functions for both coordinate system.
 *
 * Or you can use [toUtmOrUps] and [toPointCoordinates] and it will choose between the coordinate systems for you.
 *
 * @see [Wikipedia-entry on UTM](https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system)
 * @see [Wikipedia-entry on UPS](https://en.wikipedia.org/wiki/Universal_polar_stereographic_coordinate_system)
 *
 */
data class UtmCoordinate(
    val longitudeZone: Int, val latitudeZoneLetter: Char, val easting: Double, val northing: Double
) {
    override fun toString(): String {
        return "$longitudeZone $latitudeZoneLetter $easting $northing"
    }
}

val UtmCoordinate.isUps get() = latitudeZoneLetter in listOf('A', 'B', 'Y', 'Z')
val UtmCoordinate.isUtm get() = !isUps

val UtmCoordinate.isSouth get() = latitudeZoneLetter < 'N'
val UtmCoordinate.isNorth get() = !isSouth

internal val utmRegex = "(([0-9]{1,2})\\s*([a-zA-Z])\\s+(\\d*\\.?\\d+)\\s+(\\d*\\.?\\d+))".toRegex()

fun String.parseUTM(): UtmCoordinate? {
    return utmRegex.matchEntire(this)?.let {
        UtmCoordinate(
            longitudeZone = it.groups[2]!!.value.toInt(),
            latitudeZoneLetter = it.groups[3]!!.value.uppercase()[0],
            easting = it.groups[4]!!.value.toDouble(),
            northing = it.groups[5]!!.value.toDouble()
        )
    }
}

fun String.findUTMCoordinates(): List<UtmCoordinate> {
    return utmRegex.findAll(this).map {
        UtmCoordinate(
            longitudeZone = it.groups[2]!!.value.toInt(),
            latitudeZoneLetter = it.groups[3]!!.value.uppercase()[0],
            easting = it.groups[4]!!.value.toDouble(),
            northing = it.groups[5]!!.value.toDouble()
        )
    }.toList()
}

val String.utmAsWgs84Coordinate get() = parseUTM()?.utmToPointCoordinates()

/**
 * Returns true if the position indicated by the coordinates is
 * north of the northern limit of the UTM grid (84 degrees).
 */
private fun isNorthPolar(latLong: PointCoordinates): Boolean {
    return latLong.latitude > 84.0
}

/**
 * Returns true if the position indicated by the coordinates is
 * south of the southern limit of the UTM grid (-80 degrees).
 */
private fun isSouthPolar(latLong: PointCoordinates): Boolean {
    return latLong.latitude < -80.0
}

/**
 * Returns the UTM/UPS latitude zone identifier for the specified coordinates.
 *
 * @param latLong The coordinates.
 * @return the latitude zone character.
 */
private fun getLatitudeZoneLetter(latLong: PointCoordinates): Char {
    return if (isNorthPolar(latLong)) {
        if (latLong.longitude < 0) {
            return 'Y';
        } else {
            return 'Z';
        }
    } else {
        if (isSouthPolar(latLong)) {
            if (latLong.longitude < 0) {
                return 'A';
            } else {
                return 'B';
            }
        } else {
            val latitude = latLong.latitude
            when {
                latitude < -72 -> 'C'
                latitude < -64 -> 'D'
                latitude < -56 -> 'E'
                latitude < -48 -> 'F'
                latitude < -40 -> 'G'
                latitude < -32 -> 'H'
                latitude < -24 -> 'J'
                latitude < -16 -> 'K'
                latitude < -8 -> 'L'
                latitude < 0 -> 'M'
                latitude < 8 -> 'N'
                latitude < 16 -> 'P'
                latitude < 24 -> 'Q'
                latitude < 32 -> 'R'
                latitude < 40 -> 'S'
                latitude < 48 -> 'T'
                latitude < 56 -> 'U'
                latitude < 64 -> 'V'
                latitude < 72 -> 'W'
                else -> 'X'
            }
        }
    }
}

/**
 * Returns the UTM/UPS longitude zone number for the specified
 * coordinates.
 *
 * @param latLong  The coordinates.
 * @return the longitude zone number.
 */
private fun getLongitudeZone(latLong: PointCoordinates): Int {

    // UPS longitude zones
    val longitude = latLong.longitude
    return if (isNorthPolar(latLong) || isSouthPolar(latLong)) {
        if (longitude < 0.0) {
            30
        } else {
            31
        }
    } else {
        val latitudeZone: Char = getLatitudeZoneLetter(latLong)
        when {
            latitudeZone == 'X' && longitude > 0.0 && longitude < 42.0 -> {
                // X latitude exceptions
                when {
                    longitude < 9.0 -> {
                        31
                    }

                    longitude < 21.0 -> {
                        33
                    }

                    longitude < 33.0 -> {
                        35
                    }

                    else -> {
                        37
                    }
                }
            }

            latitudeZone == 'V' && longitude > 0.0 && longitude < 12.0 -> {
                // V latitude exceptions
                if (longitude < 3.0) {
                    31
                } else {
                    32
                }
            }

            else -> {
                ((longitude + 180) / 6).toInt() + 1
            }
        }
    }
}

/**
 * Returns the central meridian (in radians) for the specified
 * UTM/UPS zone.
 * @param longitudeZone The UTM/UPS longitude zone number.
 * @param latitudeZone  The UTM/UPS latitude zone character.
 * @return The central meridian for the specified zone.
 */
private fun getCentralMeridian(longitudeZone: Int, latitudeZone: Char): Double {
    // polar zones
    if (latitudeZone < 'C' || latitudeZone > 'X') {
        return 0.0
    }
    // X latitude zone exceptions
    // Svalbard exceptions for 'X' latitude zone (Svalbard)
    if (latitudeZone == 'X') {
        when (longitudeZone) {
            31 -> return toRadians(9.0)
            33 -> return toRadians(15.0)
            35 -> return toRadians(27.0)
            37 -> return toRadians(33.0)
        }
    }
    // V latitude zone exceptions (Norway)
    if (latitudeZone == 'V') {
        when (longitudeZone) {
            31 -> return toRadians(3.0)
            32 -> return toRadians(9.0)
        }
    }
    return toRadians(((longitudeZone - 1) * 6 - 180 + 3).toDouble())
}

/**
 * Converts to UTM or UPS and selects the coordinate system based on the latitude.
 */
fun PointCoordinates.toUtmOrUps(): UtmCoordinate {
    return if (latitude < UTM_SOUTHERN_LIMIT || latitude > UTM_NORTHERN_LIMIT) {
        toUpsCoordinate()
    } else {
        toUtmCoordinate()
    }
}

fun UtmCoordinate.toPointCoordinates(): PointCoordinates {
    return if (isUps) upsToPointCoordinates() else utmToPointCoordinates()
}

fun PointCoordinates.toUtmCoordinate(): UtmCoordinate {
    if (latitude < UTM_SOUTHERN_LIMIT || latitude > UTM_NORTHERN_LIMIT) {
        error("$latitude is outside UTM supported latitude range of $UTM_SOUTHERN_LIMIT - $UTM_NORTHERN_LIMIT. You should use the UPS coordinate system")
    }

    val latitudeZone: Char = getLatitudeZoneLetter(this)
    val longitudeZone: Int = getLongitudeZone(this)
    val phi: Double = toRadians(this.latitude)
    val cosPhi = cos(phi)
    val cos2Phi = cosPhi * cosPhi
    val cos3Phi = cos2Phi * cosPhi
    val cos4Phi = cos3Phi * cosPhi
    val cos5Phi = cos4Phi * cosPhi
    val cos6Phi = cos5Phi * cosPhi
    val cos7Phi = cos6Phi * cosPhi
    val cos8Phi = cos7Phi * cosPhi
    val tanPhi = tan(phi)
    val tan2Phi = tanPhi * tanPhi
    val tan4Phi = tan2Phi * tan2Phi
    val tan6Phi = tan4Phi * tan2Phi
    val eb2 = ReferenceEllipsoid.WGS84.secondEccentricitySquared
    val eb4 = eb2 * eb2
    val eb6 = eb4 * eb2
    val eb8 = eb6 * eb2
    val e2c2 = eb2 * cos2Phi
    val e4c4 = eb4 * cos4Phi
    val e6c6 = eb6 * cos6Phi
    val e8c8 = eb8 * cos8Phi
    val t2e2c2 = tan2Phi * e2c2
    val t2e4c4 = tan2Phi * e4c4
    val t2e6c6 = tan2Phi * e6c6
    val t2e8c8 = tan2Phi * e8c8
    val nu = ReferenceEllipsoid.WGS84.verticalRadiusOfCurvatureRadians(phi)
    val kn1 = K0 * nu * sin(phi)
    val t1 = K0 * ReferenceEllipsoid.WGS84.meridionalArcRadians(phi)
    val t2 = kn1 * cosPhi / 2.0
    val t3 = (kn1 * cos3Phi / 24.0
            * (5.0 - tan2Phi + 9.0 * e2c2 + 4.0 * e4c4))
    val t4 = (kn1 * cos5Phi / 720.0
            * (61.0 - 58.0 * tan2Phi + tan4Phi + 270.0 * e2c2 - 330.0
            * t2e2c2 + 445.0 * e4c4 - 680.0 * t2e4c4 + 324.0 * e6c6
            - 600.0 * t2e6c6 + 88.0 * e8c8 - 192.0 * t2e8c8))
    val t5 = (kn1 * cos7Phi / 40320.0
            * (1385.0 - 3111.0 * tan2Phi + 543.0 * tan4Phi - tan6Phi))
    val kn2 = K0 * nu
    val t6 = kn2 * cosPhi
    val t7 = kn2 * cos3Phi / 6.0 * (1.0 - tan2Phi + e2c2)
    val t8 = (kn2 * cos5Phi / 120.0
            * (5.0 - 18.0 * tan2Phi + tan4Phi + 14.0 * e2c2 - 58.0 * t2e2c2
            + 13.0 * e4c4 - 64.0 * t2e4c4 + 4.0 * e6c6 - 24.0 * t2e6c6))
    val t9 = (kn2 * cos7Phi / 50.40
            * (61.0 - 479.0 * tan2Phi + 179.0 * tan4Phi - tan6Phi))
    val lambda: Double = toRadians(this.longitude)
    val lambda0 = getCentralMeridian(longitudeZone, latitudeZone)
    val dL = lambda - lambda0
    val dL2 = dL * dL
    val dL3 = dL2 * dL
    val dL4 = dL3 * dL
    val dL5 = dL4 * dL
    val dL6 = dL5 * dL
    val dL7 = dL6 * dL
    val dL8 = dL7 * dL
    val falseNorthing: Double = if (phi < 0.0) {
        // southern hemisphere -- add false northing
        UTM_FALSE_NORTHING
    } else {
        // northern hemisphere -- no false northing
        0.0
    }
    val falseEasting = UTM_FALSE_EASTING
    val northing = falseNorthing + t1 + dL2 * t2 + dL4 * t3 + (dL6
            * t4) + dL8 * t5
    val easting = falseEasting + dL * t6 + dL3 * t7 + dL5 * t8 + dL7 * t9
    return UtmCoordinate(
        longitudeZone = longitudeZone,
        latitudeZoneLetter = latitudeZone,
        easting = easting.roundDecimals(2),
        northing = northing.roundDecimals(2)
    )
}


fun UtmCoordinate.utmToPointCoordinates(): PointCoordinates {
    val northing: Double = if (this.latitudeZoneLetter < 'N') {
        // southern hemisphere
        this.northing - UTM_FALSE_NORTHING
    } else {
        // northern hemisphere
        this.northing
    }
    // footpoint latitude
    val arc0 = northing / K0
    var rho: Double = ReferenceEllipsoid.WGS84.meridionalRadiusOfCurvatureRadians(0.0)
    var phi = arc0 / rho
    for (i in 0..4) {
        val arc: Double = ReferenceEllipsoid.WGS84.meridionalArcRadians(phi)
        rho = ReferenceEllipsoid.WGS84.meridionalRadiusOfCurvatureRadians(phi)
        val diff = (arc0 - arc) / rho
        if (abs(diff) < phi.ulp) {
            break
        }
        phi += diff
    }
    val cosPhi = cos(phi)
    val cos2Phi = cosPhi * cosPhi
    val cos3Phi = cos2Phi * cosPhi
    val cos4Phi = cos3Phi * cosPhi
    val cos5Phi = cos4Phi * cosPhi
    val cos6Phi = cos5Phi * cosPhi
    val cos7Phi = cos6Phi * cosPhi
    val cos8Phi = cos7Phi * cosPhi
    val tanPhi = tan(phi)
    val tan2Phi = tanPhi * tanPhi
    val tan4Phi = tan2Phi * tan2Phi
    val tan6Phi = tan4Phi * tan2Phi
    val eb2 = ReferenceEllipsoid.WGS84.secondEccentricitySquared
    val eb4 = eb2 * eb2
    val eb6 = eb4 * eb2
    val eb8 = eb6 * eb2
    val e2c2 = eb2 * cos2Phi
    val e4c4 = eb4 * cos4Phi
    val e6c6 = eb6 * cos6Phi
    val e8c8 = eb8 * cos8Phi
    val t2e2c2 = tan2Phi * e2c2
    val t2e4c4 = tan2Phi * e4c4
    val t2e6c6 = tan2Phi * e6c6
    val t2e8c8 = tan2Phi * e8c8
    val t4e2c2 = tan4Phi * e2c2
    val t4e4c4 = tan4Phi * e4c4
    val nu: Double = ReferenceEllipsoid.WGS84.verticalRadiusOfCurvatureRadians(phi)
    val nu2 = nu * nu
    val nu3 = nu2 * nu
    val nu5 = nu3 * nu2
    val nu7 = nu5 * nu2
    val lambda0 = getCentralMeridian(
        this.longitudeZone, this.latitudeZoneLetter
    )
    val dE: Double = this.easting - UTM_FALSE_EASTING
    val dE2 = dE * dE
    val dE3 = dE2 * dE
    val dE4 = dE3 * dE
    val dE5 = dE4 * dE
    val dE6 = dE5 * dE
    val dE7 = dE6 * dE
    val dE8 = dE7 * dE
    val t10 = tanPhi / (2.0 * rho * nu * K02)
    val t11 = (tanPhi / (24.0 * rho * nu3 * K04)
            * (5.0 + 3.0 * tan2Phi + e2c2 - 9.0 * t2e2c2 - 4.0 * e4c4))
    val t12 = (tanPhi
            / (720.0 * rho * nu5 * K06)
            * (61.0 + 90.0 * tan2Phi + 45.0 * tan4Phi + 46.0 * e2c2 - (252.0
            * t2e2c2) - 90.0 * t4e2c2 - 3.0 * e4c4 - 66.0 * t2e4c4 + 225.0 * t4e4c4 + 100.0 * e6c6 + 84.0 * t2e6c6 + (88.0
            * e8c8) - 192.0 * t2e8c8))
    val t13 = (tanPhi
            / (40320.0 * rho * nu7 * K08)
            * (1385.0 + 3633.0 * tan2Phi + 4095.0 * tan4Phi + 1575.0 * tan6Phi))
    val t14 = 1.0 / (cosPhi * nu * K0)
    val t15 = (1.0 / (6.0 * cosPhi * nu3 * K03)
            * (1.0 + 2.0 * tan2Phi + e2c2))
    val t16 = (1.0
            / (120.0 * cosPhi * nu5 * K05)
            * (5.0 + 28.0 * tan2Phi + 24.0 * tan4Phi + 6.0 * e2c2 + (8.0
            * t2e2c2) - 3.0 * e4c4 + 4.0 * t2e4c4 - 4.0 * e6c6 + 24.0 * t2e6c6))
    val t17 = (1.0 / (5040.0 * cosPhi * nu7 * K07)
            * (61.0 + 662.0 * tan2Phi + 1320.0 * tan4Phi + 720.0 * tan6Phi))
    val latitude = phi - dE2 * t10 + dE4 * t11 - dE6 * t12 + dE8 * t13
    val longitude = (lambda0 + dE * t14 - dE3 * t15 + dE5 * t16 - dE7 * t17)
    return doubleArrayOf(
        fromRadians(longitude),
        fromRadians(latitude)
    ).normalize()
}

/**
 * Converts to UPS format.
 *
 * IMPORTANT This code was adapted from the same source that I got the UTM conversion from.
 *
 * However:
 *
 * - the original did not include usable tests
 * - I've not been able to find an authoritative source of any example UPS coordinates to test with
 * - I've found and fixed several bugs in the UTM implementation where I did have access to those
 */
fun PointCoordinates.toUpsCoordinate(): UtmCoordinate {
    if (latitude in UTM_SOUTHERN_LIMIT..UTM_NORTHERN_LIMIT) {
        error("$latitude is outside UPS supported latitude range of [-90 - $UTM_SOUTHERN_LIMIT] or [$UTM_NORTHERN_LIMIT - 90]. You should use UTM")
    }

    val ellipsoid = ReferenceEllipsoid.WGS84
    val latitudeZone: Char = getLatitudeZoneLetter(this)
    val longitudeZone = getLongitudeZone(this)
    val latitude: Double = toRadians(latitude)
    val sign = sign(latitude)
    val phi = abs(latitude)
    val lambda: Double = toRadians(longitude)
    val a: Double = ellipsoid.semimajorAxis
    val e = ellipsoid.eccentricity
    val e2 = ellipsoid.eccentricitySquared
    val c0 = (2.0 * a / sqrt(1.0 - e2)
            * ((1.0 - e) / (1.0 + e)).pow(e / 2.0))
    val eSinPhi = e * sin(phi)
    val tz = ((1 + eSinPhi) / (1 - eSinPhi)).pow(e / 2.0) * tan(PI / 4.0 - phi / 2.0)
    val radius = UPS_SCALE_FACTOR * c0 * tz
    val falseNorthing = UPS_FALSE_NORTHING
    val northing: Double
    northing = if (sign > 0) {
        falseNorthing - radius * cos(lambda)
    } else {
        falseNorthing + radius * cos(lambda)
    }
    val falseEasting = UPS_FALSE_EASTING
    val easting = falseEasting + radius * sin(lambda)
    return UtmCoordinate(longitudeZone, latitudeZone, easting, northing)
}

fun UtmCoordinate.upsToPointCoordinates(): PointCoordinates {
    val ellipsoid = ReferenceEllipsoid.WGS84
    val northernHemisphere: Boolean = latitudeZoneLetter > 'N'
    val dN: Double = (northing - UPS_FALSE_NORTHING)
    val dE: Double = (easting - UPS_FALSE_EASTING)
    // check for zeroes (the poles)
    if (dE == 0.0 && dN == 0.0) {
        return if (northernHemisphere) {
            doubleArrayOf(0.0, 90.0)
        } else {
            doubleArrayOf(0.0, -90.0)
        }
    }
    // compute longitude
    val longitude: Double = if (northernHemisphere) {
        atan2(dE, -dN)
    } else {
        atan2(dE, dN)
    }

    // compute latitude
    val a: Double = ellipsoid.semimajorAxis
    val e: Double = ellipsoid.eccentricity
    val e2: Double = ellipsoid.eccentricitySquared
    val e4 = e2 * e2
    val e6 = e4 * e2
    val e8 = e6 * e2
    val aBar = e2 / 2.0 + 5.0 * e4 / 24.0 + e6 / 12.0 + (13 * e8
            / 360.0)
    val bBar = 7.0 * e4 / 48.0 + 29.0 * e6 / 240.0 + (811.0 * e8
            / 11520.0)
    val cBar = 7.0 * e6 / 120.0 + 81.0 * e8 / 1120.0
    val dBar = 4279 * e8 / 161280.0
    val c0: Double = (2.0 * a / sqrt(1.0 - e2)
            * ((1.0 - e) / (1.0 + e)).pow(e / 2.0))
    val r: Double = if (dE == 0.0) {
        dN
    } else if (dN == 0.0) {
        dE
    } else if (dN < dE) {
        dE / sin(longitude)
    } else {
        dN / cos(longitude)
    }
    val radius = abs(r)
    val chi: Double = PI / 2.0 - 2.0 * atan2(radius, UPS_SCALE_FACTOR * c0)
    val phi = chi + aBar * sin(2.0 * chi) + (bBar
            * sin(4.0 * chi)) + cBar * sin(6.0 * chi) + (dBar
            * sin(8.0 * chi))
    val latitude: Double = if (northernHemisphere) {
        phi
    } else {
        -phi
    }
    return doubleArrayOf(fromRadians(longitude), fromRadians(latitude)).normalize()
}


/**
 *
 *  The ReferenceEllipsoid class defines a geodetic reference ellipsoid
 * used as a standard for geodetic measurements. The World Geodetic System
 * 1984 (WGS84) ellipsoid is the current standard for most geographic and
 * geodetic coordinate systems, including GPS. The WGS84 ellipsoid is
 * provided as a static instance of this class.
 *
 *
 *  The ellipsoid (actually an oblate spheroid) is uniquely specified by
 * two parameters, the semimajor (or equatorial) radius and the ellipticity
 * or flattening. In practice, the reciprocal of the flattening is
 * specified.
 *
 *
 *  The ellipsoid is an approximation of the shape of the earth. Although
 * not exact, the ellipsoid is much more accurate than a spherical
 * approximation and is still mathematically simple. The *geoid* is
 * a still closer approximation of the shape of the earth (intended to
 * represent the mean sea level), and is generally specified by it's
 * deviation from the ellipsoid.
 *
 *
 *  Different reference ellipsoids give more or less accurate results at
 * different locations, so it was previously common for different nations
 * to use ellipsoids that were more accurate for their areas. More recent
 * efforts have provided ellipsoids with better overall global accuracy,
 * such as the WGS84 ellipsiod, and these have now largely supplanted
 * the others.
 *
 * @author Jilles van Gurp (adapted the code to kotlin)
 * @author Paul D. Anderson
 * @version 3.0, February 18, 2006
 */
class ReferenceEllipsoid(private val a: Double, inverseFlattening: Double) {

    /**
     * Flattening or ellipticity of this reference ellipsoid.
     */
    val flattening: Double = 1.0 / inverseFlattening

    val b = a * (1.0 - flattening)

    /**
     * Square of the (first) eccentricity. This number is frequently
     * used in ellipsoidal calculations.
     */
    val eccentricitySquared = flattening * (2.0 - flattening)


    val secondEccentricitySquared = eccentricitySquared / (1.0 - eccentricitySquared)

    val eccentricity = sqrt(eccentricitySquared)
    val semimajorAxis = a
    val semiminorAxis = a * (1.0 - (1.0 / inverseFlattening))

    /**
     * Returns the *radius of curvature in the prime vertical*
     * for this reference ellipsoid at the specified latitude [phi] in radians.
     */
    fun verticalRadiusOfCurvatureRadians(phi: Double): Double {
        return a / sqrt(1.0 - eccentricitySquared * sin(phi).pow(2))
    }

    /**
     * Returns the *radius of curvature in the meridian*
     * for this reference ellipsoid at the specified latitude [phi] in radians.
     ** */
    fun meridionalRadiusOfCurvatureRadians(phi: Double): Double {
        return (verticalRadiusOfCurvatureRadians(phi) / (1.0 + secondEccentricitySquared * cos(phi).pow(2)))
    }

    /**
     * Returns the meridional arc, the true meridional distance on the
     * ellipsoid from the equator to the specified latitude [phi] in radians, in meters.
     */
    fun meridionalArcRadians(phi: Double): Double {
        val sin2Phi = sin(2.0 * phi)
        val sin4Phi = sin(4.0 * phi)
        val sin6Phi = sin(6.0 * phi)
        val sin8Phi = sin(8.0 * phi)
        val n = flattening / (2.0 - flattening)
        val n2 = n * n
        val n3 = n2 * n
        val n4 = n3 * n
        val n5 = n4 * n
        val n1n2 = n - n2
        val n2n3 = n2 - n3
        val n3n4 = n3 - n4
        val n4n5 = n4 - n5
        val ap = a * (1.0 - n + 5.0 / 4.0 * n2n3 + 81.0 / 64.0 * n4n5)
        val bp = 3.0 / 2.0 * a * (n1n2 + 7.0 / 8.0 * n3n4 + 55.0 / 64.0 * n5)
        val cp = 15.0 / 16.0 * a * (n2n3 + 3.0 / 4.0 * n4n5)
        val dp = 35.0 / 48.0 * a * (n3n4 + 11.0 / 16.0 * n5)
        val ep = 315.0 / 512.0 * a * n4n5
        return ap * phi - bp * sin2Phi + cp * sin4Phi - dp * sin6Phi + ep * sin8Phi
    }

    companion object {
        /**
         * The World Geodetic System 1984 reference ellipsoid.
         */
        val WGS84 = ReferenceEllipsoid(6378137.0, 298.257223563)
    }
}