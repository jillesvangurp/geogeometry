package com.jillesvangurp.geo

import kotlin.math.floor
import kotlin.math.roundToInt

//private val eastingLetters = "ABCDEFGHJKLMNPQRSTUVWXYZ" // Excludes I and O
//private val northingLetters = "ABCDEFGHJKLMNPQRSTUV"    // Excludes I and O

data class MgrsCoordinate(
    val longitudeZone: Int,
    val latitudeZoneLetter: Char,
    val firstLetter: Char, // aka col
    val secondLetter: Char, //aka row
    val easting: Int,
    val northing: Int
) {
    override fun toString(): String {
        return "$longitudeZone$latitudeZoneLetter $firstLetter$secondLetter $easting $northing"
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

    val actualCol = if (col == 0) 7 else col - 1
    val actualRow = if (row == 0) 19 else row - 1
    return set.colLetters()[actualCol] to set.rowLetters()[actualRow]
}

fun UtmCoordinate.convertUTMToMGRS(): MgrsCoordinate {
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
