package com.jillesvangurp.geo


/*
 * Converted to Kotlin and adapted from:
 * https://github.com/tarelli/jscience/blob/master/src/org/jscience/geography/coordinates/crs/ReferenceEllipsoid.java
 *
 * JScience - Java(TM) Tools and Libraries for the Advancement of Sciences.
 * Copyright (C) 2006 - JScience (http://jscience.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
import com.jillesvangurp.geo.GeoGeometry.Companion.fromRadians
import com.jillesvangurp.geo.GeoGeometry.Companion.roundDecimals
import com.jillesvangurp.geo.GeoGeometry.Companion.toRadians
import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
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
private const val UPS_SCALE_FACTOR = 0.994

/**
 * The UPS "false easting" value. This quantity is added to the true
 * easting to avoid using negative numbers in the coordinates.
 */
private const val UPS_FALSE_EASTING = 2000000.0


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
 * Class representing UTM-coordinates. Based on code from stack overflow and adapted from a gist by yazdipour
 *
 * @see [https://gist.github.com/yazdipour/6231fcc7d1da8588601da2395dc3cb78]
 * @see [Stack Overflow](https://stackoverflow.com/questions/176137/java-convert-lat-lon-to-utm)
 *
 * @see [Wikipedia-entry on UTM](https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system)
 *
 * @author Rolf Rander
 */
data class UTM(
    val zone: Int = 0, val letter: Char = 0.toChar(), val easting: Double = 0.0, val northing: Double = 0.0
) {
    override fun toString(): String {
        return "$zone $letter $easting $northing"
    }
}

val utmRegex = "(([0-9]{1,2})\\s*([a-zA-Z])\\s+(\\d*\\.?\\d+)\\s+(\\d*\\.?\\d+))".toRegex()

fun String.parseUTM(): UTM? {
    return utmRegex.matchEntire(this)?.let {
        UTM(
            zone = it.groups[2]!!.value.toInt(),
            letter = it.groups[3]!!.value.uppercase()[0],
            easting = it.groups[4]!!.value.toDouble(),
            northing = it.groups[5]!!.value.toDouble()
        )
    }
}

fun String.findUTMCoordinates(): List<UTM> {
    return utmRegex.findAll(this).map {
        UTM(
            zone = it.groups[2]!!.value.toInt(),
            letter = it.groups[3]!!.value.uppercase()[0],
            easting = it.groups[4]!!.value.toDouble(),
            northing = it.groups[5]!!.value.toDouble()
        )
    }.toList()
}

val String.utmAsWgs84 get() = parseUTM()?.toWgs84()

val PointCoordinates.geoJson get() = "[$longitude,$latitude]"
val PointCoordinates.format get() = run {
    val ns = if (latitude < 0) 'S' else 'N'
    val ew = if (longitude < 0) 'W' else 'E'
    "${abs(latitude)}$ns ${abs(longitude)}$ew"
}


/**
 * Returns true if the position indicated by the coordinates is
 * north of the northern limit of the UTM grid (84 degrees).
 *
 * @param latLong The coordinates.
 * @return True if the latitude is greater than 84 degrees.
 */
fun isNorthPolar(latLong: PointCoordinates): Boolean {
    return latLong.latitude > 84.0
}

/**
 * Returns true if the position indicated by the coordinates is
 * south of the southern limit of the UTM grid (-80 degrees).
 *
 * @param latLong The coordinates.
 * @return True if the latitude is less than -80 degrees.
 */
fun isSouthPolar(latLong: PointCoordinates): Boolean {
    return latLong.latitude < -80.0
}

/**
 * Returns the UTM/UPS latitude zone identifier for the specified coordinates.
 *
 * @param latLong The coordinates.
 * @return the latitude zone character.
 */
fun getLatitudeZoneLetter(latLong: PointCoordinates): Char {
    val latitude = latLong.latitude
    return when {
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

/**
 * Returns the UTM/UPS longitude zone number for the specified
 * coordinates.
 *
 * @param latLong  The coordinates.
 * @return the longitude zone number.
 */
fun getLongitudeZone(latLong: PointCoordinates): Int {

    // UPS longitude zones
    if (isNorthPolar(latLong) || isSouthPolar(latLong)) {
        return if (latLong.longitude < 0.0) {
            30
        } else {
            31
        }
    }
    val latitudeZone: Char = getLatitudeZoneLetter(latLong)
    // X latitude exceptions
    if (latitudeZone == 'X' && latLong.longitude > 0.0 && latLong.longitude < 42.0) {
        if (latLong.longitude < 9.0) {
            return 31
        }
        if (latLong.longitude < 21.0) {
            return 33
        }
        return if (latLong.longitude < 33.0) {
            35
        } else {
            37
        }
    }
    // V latitude exceptions
    return if (latitudeZone == 'V' && latLong.longitude > 0.0 && latLong.longitude < 12.0) {
        if (latLong.longitude < 3.0) {
            31
        } else {
            32
        }
    } else ((latLong.longitude + 180) / 6).toInt() + 1
}

/**
 * Returns the central meridian (in radians) for the specified
 * UTM/UPS zone.
 * @param longitudeZone The UTM/UPS longitude zone number.
 * @param latitudeZone  The UTM/UPS latitude zone character.
 * @return The central meridian for the specified zone.
 */
fun getCentralMeridian(longitudeZone: Int, latitudeZone: Char): Double {
    // polar zones
    if (latitudeZone < 'C' || latitudeZone > 'X') {
        return 0.0
    }
    // X latitude zone exceptions
    if (latitudeZone == 'X' && longitudeZone > 31 && longitudeZone <= 37) {
        return toRadians((longitudeZone - 1) * 6 - 180 + 4.5)
    }
    // V latitude zone exceptions
    if (longitudeZone == 'V'.code) {
        if (latitudeZone.code == 31) {
            return 1.5
        } else if (latitudeZone.code == 32) {
            return 7.5
        }
    }
    return toRadians(((longitudeZone - 1) * 6 - 180 + 3).toDouble())
}


fun PointCoordinates.toUTM(): UTM {
    if(latitude < UTM_SOUTHERN_LIMIT || latitude > UTM_NORTHERN_LIMIT) {
        error("$latitude is outside UTM supported latitude range of $UTM_SOUTHERN_LIMIT - $UTM_NORTHERN_LIMIT")
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
    return UTM(
        zone = longitudeZone,
        letter = latitudeZone,
        easting = easting.roundDecimals(2),
        northing = northing.roundDecimals(2)
    )
}


fun UTM.toWgs84(): PointCoordinates {
    val northing: Double = if (this.letter < 'N') {
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
        this.zone, this.letter
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
    return doubleArrayOf(fromRadians(longitude), fromRadians(latitude))
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
 * @author Paul D. Anderson
 * @version 3.0, February 18, 2006
 */
class ReferenceEllipsoid(private val a: Double, inverseFlattening: Double) {
    private val b: Double

    /**
     * Returns the flattening or ellipticity of this reference ellipsoid.
     *
     * @return The flattening.
     */
    val flattening: Double

    /**
     * Returns the square of the (first) eccentricity. This number is frequently
     * used in ellipsoidal calculations.
     *
     * @return The square of the eccentricity.
     */
    val eccentricitySquared: Double

    /**
     * Returns the (first) eccentricity of this reference ellipsoid.
     *
     * @return The eccentricity.
     */
    val eccentricity: Double

    /**
     * Returns the square of the second eccentricity of this reference ellipsoid.
     * This number is frequently used in ellipsoidal calculations.
     *
     * @return The square of the second eccentricity.
     */
    val secondEccentricitySquared: Double
    private var _semimajorAxis: Double? = null
    private var _semiminorAxis: Double? = null

    /**
     * Constructs an instance of a reference ellipsoid.
     *
     * @param semimajorAxis The semimajor or equatorial radius of this
     * reference ellipsoid, in meters.
     * @param inverseFlattening The reciprocal of the ellipticity or flattening
     * of this reference ellipsoid (dimensionless).
     */
    init {
        flattening = 1.0 / inverseFlattening
        b = a * (1.0 - flattening)
        eccentricitySquared = flattening * (2.0 - flattening)
        eccentricity = sqrt(eccentricitySquared)
        secondEccentricitySquared = eccentricitySquared / (1.0 - eccentricitySquared)
    }

    val semimajorAxis: Double
        /**
         * Returns the semimajor or equatorial radius of this reference ellipsoid.
         *
         * @return The semimajor radius.
         */
        get() {
            if (_semimajorAxis == null) {
                _semimajorAxis = a

            }
            return a
        }

    /**
     * Returns the semiminor or polar radius of this reference ellipsoid.
     *
     * @return  The semiminor radius.
     */
    fun getsSemiminorAxis(): Double? {
        if (_semiminorAxis == null) {
            _semiminorAxis = b
        }
        return _semiminorAxis
    }

    /**
     * Returns the *radius of curvature in the prime vertical*
     * for this reference ellipsoid at the specified latitude.
     *
     * @param phi The local latitude (radians).
     * @return The radius of curvature in the prime vertical (meters).
     */
    fun verticalRadiusOfCurvatureRadians(phi: Double): Double {
        return a / sqrt(1.0 - eccentricitySquared * sqr(sin(phi)))
    }

    /**
     * Returns the *radius of curvature in the meridian*
     * for this reference ellipsoid at the specified latitude.
     *
     * @param phi The local latitude (in radians).
     * @return  The radius of curvature in the meridian (in meters).
     ** */
    fun meridionalRadiusOfCurvatureRadians(phi: Double): Double {
        return (verticalRadiusOfCurvatureRadians(phi) / (1.0 + secondEccentricitySquared * sqr(cos(phi))))
    }

    /**
     * Returns the meridional arc, the true meridional distance on the
     * ellipsoid from the equator to the specified latitude, in meters.
     *
     * @param phi   The local latitude (in radians).
     * @return  The meridional arc (in meters).
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

        /**
         * Geodetic Reference System 1980 ellipsoid.
         */
        val GRS80 = ReferenceEllipsoid(6378137.0, 298.257222101)

        /**
         * The World Geodetic System 1972 reference ellipsoid.
         */
        val WGS72 = ReferenceEllipsoid(6378135.0, 298.26)

        /**
         * The International 1924 reference ellipsoid, one of the earliest
         * "global" ellipsoids.
         */
        val INTERNATIONAL1924 = ReferenceEllipsoid(6378388.0, 297.0)
        private fun sqr(x: Double): Double {
            return x * x
        }
    }
}