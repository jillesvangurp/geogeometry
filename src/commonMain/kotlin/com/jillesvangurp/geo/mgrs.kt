package com.jillesvangurp.geo

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * This mgrs code is the result of me doing a bit of dead code archeology on various
 * Java code repositories that appear abandoned.
 *
 * All of them had issues, and Java is just hard to read. Objectively, this looks a lot cleaner.
 *
 */

enum class MgrsPrecision(val divisor: Int) {
    TEN_KM(10000),
    ONE_KM(1000),
    HUNDRED_M(100),
    TEN_M(10),
    ONE_M(1)
}
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
     * USNG is the human readable version of mgrs which has no spaces.
     */
    fun usng(precision: MgrsPrecision=MgrsPrecision.ONE_M) =
        "$longitudeZone$latitudeZoneLetter $firstLetter$secondLetter ${easting / precision.divisor} ${northing / precision.divisor}"


    fun mgrs(precision: MgrsPrecision=MgrsPrecision.ONE_M) =
        "$longitudeZone$latitudeZoneLetter$firstLetter$secondLetter${easting/precision.divisor}${northing/precision.divisor}"

}

private val mgrsRegex = "([0-9]+)\\s*([A-Z])\\s*([A-Z])\\s*([A-Z])\\s*([0-9]{1,5}\\s*[0-9]{1,5})".toRegex()
fun String.parseMgrs(): MgrsCoordinate? {
    return mgrsRegex.find(this)?.let { match ->
        val groups = match.groups
        val longitudeZone = groups[1]!!.value.toInt()
        val latitudeZoneLetter = groups[2]!!.value[0]
        val firstLetter = groups[3]!!.value[0]
        val secondLetter = groups[4]!!.value[0]
        val numbers = groups[5]!!.value.replace(" ","")
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

fun Int.setForZone(): Int {
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

private const val BLOCK_SIZE = 100000


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

fun UtmCoordinate.lookupGridLetters(): Pair<Char, Char> {
    var row = 1
    var n = northing.roundToInt()
    while (n >= BLOCK_SIZE) {
        n -= BLOCK_SIZE
        row++
    }
    row %= 20

    var col = 0
    var e = easting.roundToInt()
    while (e >= BLOCK_SIZE) {
        e -= BLOCK_SIZE
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
 * This code mostly works but has some edge cases that somehow fail, which means that you
 * should not blindly trust this code.
 */
fun UtmCoordinate.toMgrs(): MgrsCoordinate {
    val (l1, l2) = lookupGridLetters()

    val mgrsEasting = floor(easting % BLOCK_SIZE).toInt()
    val mgrsNorthing = floor(northing % BLOCK_SIZE).toInt()
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

data class LatitudeBandConstants(
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

val eastingArray = listOf("", "AJS", "BKT", "CLU", "DMV", "ENW", "FPX", "GQY", "HRZ")

private const val TWO_MILLION = 2000000

fun MgrsCoordinate.toUtm(): UtmCoordinate {

    val bandConstants = latitudeBandConstants[latitudeZoneLetter]!!

    val utmEasting = eastingArray.withIndex().first { (i,letters) ->
        firstLetter in letters
    }.let { (i, letters) ->
        i * BLOCK_SIZE + easting
    }

    val setNumber = longitudeZone.setForZone()

    val rowLettersForZone = setNumber.rowLetters()
    var utmNorthing = (rowLettersForZone.indexOf(secondLetter) * 100000).toDouble()

    utmNorthing += bandConstants.northingOffset
    while(utmNorthing < bandConstants.minNorthing) {
        utmNorthing += TWO_MILLION
    }
    utmNorthing += northing

    return UtmCoordinate(longitudeZone, latitudeZoneLetter, utmEasting.toDouble(), utmNorthing)
}
