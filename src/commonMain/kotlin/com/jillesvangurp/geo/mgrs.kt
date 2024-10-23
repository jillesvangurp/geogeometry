package com.jillesvangurp.geo

import com.jillesvangurp.geojson.PointCoordinates
import kotlin.math.abs
import kotlin.math.floor

/*
 * This mgrs code is the result of me doing a bit of dead code archeology on various
 * Java code repositories that appear abandoned.
 *
 * All of them had issues, and Java is just hard to read for this stuff (verbose, many levels of indirection, etc.).
 *
 * This implementation does not handle UPS currently but should work for UTM coordinates in the supported latitudes for that
 *
 * With some tweaks and fixes, I think I've nailed most of them. But it's always possible that I
 * missed an edge case.
 *
 * The simple test here is that any coordinate in the UTM range should convert from and back
 * to UTM without ending up more than a few meters away. The UTMTest contains such a test that also tests
 * the conversion to and from mgrs.
 *
 * Some of the code bases I looked at:
 *
 * @see https://github.com/andreynovikov/Geo-Coordinate-Conversion-Java/blob/master/src/main/java/gov/nasa/worldwind/geom/coords/MGRSCoordConverter.java
 * @see https://github.com/ngageoint/mgrs-java/blob/master/src/main/java/mil/nga/mgrs/MGRS.java
 * @see https://github.com/OpenSextant/opensextant/blob/master/Xponents/XCoord/src/main/java/org/mitre/xcoord/MGRSFilter.java
 */

/**
 * MGRS precision for the easting and northing.
 * [MgrsCoordinate] stores everything in meter precision but can format with any of these precisions.
 */
enum class MgrsPrecision(val divisor: Int, val digits: Int) {
    TEN_KM(10000,1),
    ONE_KM(1000,2),
    HUNDRED_M(100,3),
    TEN_M(10, 4),
    ONE_M(1, 5)
}

/**
 * Represent an MGRS coordinate.
 *
 * [longitudeZone] same as in [UtmCoordinate]
 * [latitudeZoneLetter] same as in [UtmCoordinate]
 * [firstLetter] First latter of the grid inside the UTM zone. Aka. the grid column.
 * [secondLetter] Second letter of the grid inside the UTM zone. Aka. the grid row.
 * [easting] Easting inside the 100km grid
 * [northing] Northing inside the 100km grid
 */
data class MgrsCoordinate(
    val longitudeZone: Int,
    val latitudeZoneLetter: Char,
    val firstLetter: Char, // aka col
    val secondLetter: Char, //aka row
    val easting: Int,
    val northing: Int
) {
    override fun toString(): String {
        return usng(MgrsPrecision.ONE_M)
    }

    /**
     * USNG is the human-readable version of MGRS which includes spaces.
     */
    fun usng(precision: MgrsPrecision = MgrsPrecision.ONE_M): String {
        val eastingStr = (easting / precision.divisor).toString().padStart(precision.digits, '0')
        val northingStr = (northing / precision.divisor).toString().padStart(precision.digits, '0')
        return "$longitudeZone$latitudeZoneLetter $firstLetter$secondLetter $eastingStr $northingStr"
    }

    /**
     * MGRS is the compact format without spaces.
     */
    fun mgrs(precision: MgrsPrecision = MgrsPrecision.ONE_M): String {
        val eastingStr = (easting / precision.divisor).toString().padStart(precision.digits, '0')
        val northingStr = (northing / precision.divisor).toString().padStart(precision.digits, '0')
        return "$longitudeZone$latitudeZoneLetter$firstLetter$secondLetter$eastingStr$northingStr"
    }
}

private fun Int.setForZone(): Int {
    return when (this % 6) {
        0 -> 6
        1 -> 1
        2 -> 2
        3 -> 3
        4 -> 4
        5 -> 5
        else -> error("should not happen")
    }
}

private const val HUNDRED_KM = 100_000
private const val TWO_MILLION = 2_000_000
private fun Int.colLetters() = when (this) {
    1 -> "ABCDEFGH"
    2 -> "JKLMNPQR"
    3 -> "STUVWXYZ"
    4 -> "ABCDEFGH"
    5 -> "JKLMNPQR"
    6 -> "STUVWXYZ"
    else -> error("should not happen")
}

private fun Int.rowLetters() = if (this % 2 == 0) "FGHJKLMNPQRSTUVABCDE" else "ABCDEFGHJKLMNPQRSTUV"

private fun UtmCoordinate.lookupGridLetters(): Pair<Char, Char> {
    var row = 1
    // always floor in mgrs, or you might end up in the wrong grid cell
    var n = floor(northing).toInt()
    while (n >= HUNDRED_KM) {
        n -= HUNDRED_KM
        row++
    }
    row %= 20

    var col = 0
    // always floor in mgrs, or you might end up in the wrong grid cell
    var e = floor(easting).toInt()
    while (e >= HUNDRED_KM) {
        e -= HUNDRED_KM
        col++
    }
    col %= 8

    val set = longitudeZone.setForZone()

    val actualCol = if (col == 0) {
        7
    } else col - 1
    val actualRow = if (row == 0) {
        19
    } else row - 1
    return set.colLetters()[actualCol] to set.rowLetters()[actualRow]
}


/**
 * Convert to MGRS coordinate.
 *
 * After converting, you can format either as mgrs (no spaces) or usng format with
 * various precision.
 *
 * Note, this does not support coordinates in the UPS coordinate system currently.
 */
fun UtmCoordinate.toMgrs(): MgrsCoordinate {
    val (l1, l2) = lookupGridLetters()

    // always floor in mgrs, or you might end up in the wrong grid cell
    val mgrsEasting = floor(easting % HUNDRED_KM).toInt()
    val mgrsNorthing = floor(northing % HUNDRED_KM).toInt()
//    println("cols ${northing.toInt() / BLOCK_SIZE} $northing ${(northing / 1000).toInt()}")
    return MgrsCoordinate(
        longitudeZone,
        latitudeZoneLetter,
        l1,
        l2,
        mgrsEasting,
        mgrsNorthing
    )
}

private data class LatitudeBandConstants(
    val firstLetter: Char, // col
    val minNorthing: Double,
    val northLat: Double,
    val southLat: Double,
    val northingOffset: Double
)

// These is a useful set of constants I extracted from https://github.com/andreynovikov/Geo-Coordinate-Conversion-Java/blob/master/src/main/java/gov/nasa/worldwind/geom/coords/MGRSCoord.java
private val latitudeBandConstants = listOf(
    LatitudeBandConstants('C', 1100000.0, -72.0, -80.5, 0.0),
    LatitudeBandConstants('D', 2000000.0, -64.0, -72.0, 2000000.0),
    LatitudeBandConstants('E', 2800000.0, -56.0, -64.0, 2000000.0),
    LatitudeBandConstants('F', 3700000.0, -48.0, -56.0, 2000000.0),
    LatitudeBandConstants('G', 4600000.0, -40.0, -48.0, 4000000.0),
    LatitudeBandConstants('H', 5500000.0, -32.0, -40.0, 4000000.0),
    LatitudeBandConstants('J', 6400000.0, -24.0, -32.0, 6000000.0),
    LatitudeBandConstants('K', 7300000.0, -16.0, -24.0, 6000000.0),
    LatitudeBandConstants('L', 8200000.0, -8.0, -16.0, 8000000.0),
    LatitudeBandConstants('M', 9100000.0, 0.0, -8.0, 8000000.0),
    LatitudeBandConstants('N', 0.0, 8.0, 0.0, 0.0),
    LatitudeBandConstants('P', 800000.0, 16.0, 8.0, 0.0),
    LatitudeBandConstants('Q', 1700000.0, 24.0, 16.0, 0.0),
    LatitudeBandConstants('R', 2600000.0, 32.0, 24.0, 2000000.0),
    LatitudeBandConstants('S', 3500000.0, 40.0, 32.0, 2000000.0),
    LatitudeBandConstants('T', 4400000.0, 48.0, 40.0, 4000000.0),
    LatitudeBandConstants('U', 5300000.0, 56.0, 48.0, 4000000.0),
    LatitudeBandConstants('V', 6200000.0, 64.0, 56.0, 6000000.0),
    LatitudeBandConstants('W', 7000000.0, 72.0, 64.0, 6000000.0),
    LatitudeBandConstants('X', 7900000.0, 84.5, 72.0, 6000000.0)
).associateBy { it.firstLetter }

private val eastingArray = listOf("", "AJS", "BKT", "CLU", "DMV", "ENW", "FPX", "GQY", "HRZ")

/**
 * Returns the UTM coordinate for the MGRS coordinate.
 *
 * Note, this does not support coordinates in the UPS coordinate system currently.
 */
fun MgrsCoordinate.toUtm(): UtmCoordinate {
    // FIXME check if ups or utm, this implementation is for utm
    val bandConstants = latitudeBandConstants[latitudeZoneLetter]!!

    val utmEasting = eastingArray.withIndex().first { (_, letters) ->
        firstLetter in letters
    }.let { (i, _) ->
        (i * HUNDRED_KM + easting).toDouble()
    }

    val setNumber = longitudeZone.setForZone()

    val rowLettersForZone = setNumber.rowLetters()
    var utmNorthing = (rowLettersForZone.indexOf(secondLetter) * 100000).toDouble()

    utmNorthing += bandConstants.northingOffset
    while (utmNorthing < bandConstants.minNorthing) {
        utmNorthing += TWO_MILLION
    }

    utmNorthing += northing

    return UtmCoordinate(longitudeZone, latitudeZoneLetter, utmEasting, utmNorthing)
}

fun PointCoordinates.toMgrs() = toUtmCoordinate().toMgrs()
fun MgrsCoordinate.toPointCoordinate() = toUtm().toPointCoordinates()

private val mgrsRegex = "([0-9]+)\\s*([A-Z])\\s*([A-Z])\\s*([A-Z])\\s*([0-9]{1,5}\\s*[0-9]{1,5})".toRegex()

/**
 * Parses a mgrs or usng string to [MgrsCoordinate]. Returns the coordinate or null if none was found.
 *
 * Note the string may contain other things, this implementation simply looks for the first matching coordinate.
 */
fun String.parseMgrs(): MgrsCoordinate? {
    return mgrsRegex.find(this)?.let { match ->
        val groups = match.groups
        val longitudeZone = groups[1]!!.value.toInt()
        val latitudeZoneLetter = groups[2]!!.value[0]
        val firstLetter = groups[3]!!.value[0]
        val secondLetter = groups[4]!!.value[0]
        val numbers = groups[5]!!.value.replace(" ", "")
        if (numbers.length % 2 != 0) {
            null
        } else {
            val mid = numbers.length / 2
            val precision = MgrsPrecision.entries[mid - 1]
            val easting = numbers.substring(0, mid).toInt() * precision.divisor
            val northing = numbers.substring(mid).toInt() * precision.divisor
            MgrsCoordinate(longitudeZone, latitudeZoneLetter, firstLetter, secondLetter, easting, northing)
        }
    }
}

private fun roundMGRS(value: Double): Long {
    val ival = floor(value).toLong()
    val fraction = value - ival
    // double fraction = modf (value, &ivalue);
    return if (fraction > 0.5 || fraction == 0.5 && ival % 2L == 1L) ival + 1 else ival
}

data class UpsConstant(
    val latitudeZoneLetter: Char,
    val chars: List<Char>,
    val falseEasting: Int,
    val falseNorthing: Int
)

private val upsConstants = arrayOf<UpsConstant>(
    UpsConstant('A', listOf('J', 'Z', 'Z'), 800000, 800000),
    UpsConstant('B', listOf('A', 'R', 'Z'), 2000000, 800000),
    UpsConstant('Y', listOf('J', 'Z', 'P'), 800000, 1300000),
    UpsConstant('Z', listOf('A', 'J', 'P'), 2000000, 1300000),
).associateBy { it.latitudeZoneLetter }

fun UtmCoordinate.convertUPSToMGRS(): MgrsCoordinate {
    // FIXME test and convert back to UPS
    val falseEasting: Double /* False easting for 2nd letter                 */
    val falseNorthing: Double /* False northing for 3rd letter                */
    val ltr2LowValue: Char /* 2nd letter range - low number                */
    var firstLetter: Char
    var secondLetter: Char
    if (!isUps) {
        error("not a ups coordinate")
    }
    val upsConstant = upsConstants[latitudeZoneLetter]!!
    ltr2LowValue = upsConstant.chars[0]
    falseEasting = upsConstant.falseEasting.toDouble()
    falseNorthing = upsConstant.falseNorthing.toDouble()

    val grid_northing: Double = northing - falseNorthing /* Northing used to derive 3rd letter of MGRS   */

    secondLetter = (grid_northing / HUNDRED_KM).toInt().toChar()

    if (secondLetter > 'H') secondLetter += 1
    if (secondLetter > 'N') secondLetter += 1
    val gridEasting: Double = easting - falseEasting /* Easting used to derive 2nd letter of MGRS    */

    firstLetter = (ltr2LowValue + (gridEasting / HUNDRED_KM).toInt())
    if (easting < TWO_MILLION) {
        if (firstLetter > 'L') firstLetter = firstLetter + 3
        if (firstLetter > 'U') firstLetter = firstLetter + 2
    } else {
        if (firstLetter > 'C') firstLetter = firstLetter + 2
        if (firstLetter > 'H') firstLetter = firstLetter + 1
        if (firstLetter > 'L') firstLetter = firstLetter + 3
    }

    return MgrsCoordinate(
        longitudeZone,
        latitudeZoneLetter,
        firstLetter,
        secondLetter,
        floor(easting).toInt() % HUNDRED_KM,
        floor(northing).toInt() % HUNDRED_KM
    )
}
