package com.jillesvangurp.geo

import kotlin.math.floor
import kotlin.math.roundToInt

//private val eastingLetters = "ABCDEFGHJKLMNPQRSTUVWXYZ" // Excludes I and O
//private val northingLetters = "ABCDEFGHJKLMNPQRSTUV"    // Excludes I and O

data class MgrsCoordinate(
    val longitudeZone: Int,
    val latitudeZoneLetter: Char,
    val colLetter: Char,
    val rowLetter: Char,
    val easting: Int,
    val northing: Int
) {
    override fun toString(): String {
        return "$longitudeZone$latitudeZoneLetter $colLetter$rowLetter $easting $northing"
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
    return MgrsCoordinate(
        longitudeZone,
        latitudeZoneLetter,
        l1,
        l2,
        mgrsEasting,
        mgrsNorthing
    )
}

fun MgrsCoordinate.toUtm(): UtmCoordinate {
    val eastingArray = listOf("", "AJS", "BKT", "CLU", "DMV", "ENW", "FPX", "GQY", "HRZ")
    val zoneBase = listOf(
        1.1,
        2.0,
        2.8,
        3.7,
        4.6,
        5.5,
        6.4,
        7.3,
        8.2,
        9.1,
        0.0,
        0.8,
        1.7,
        2.6,
        3.5,
        4.4,
        5.3,
        6.2,
        7.0,
        7.9
    ).map { it * 1000000 }

    var utmEasting = -1
    for ((i, letters) in eastingArray.withIndex()) {
        if (colLetter in letters) {
            utmEasting = i * BLOCK_SIZE + easting
            break
        }
    }

    var utmNorthing = 0.0
    if (rowLetter != ' ') {
        utmNorthing = if (longitudeZone % 2 == 0) {
            ("FGHJKLMNPQRSTUVABCDE".indexOf(rowLetter) * 100000).toDouble()
        } else {
            ("ABCDEFGHJKLMNPQRSTUV".indexOf(rowLetter) * 100000).toDouble()
        }

        while (utmNorthing < zoneBase["CDEFGHJKLMNPQRSTUVWX".indexOf(rowLetter)-1]) {
            utmNorthing += 2000000
        }
        utmNorthing += northing

    } else {
        utmNorthing = zoneBase["CDEFGHJKLMNPQRSTUVWX".indexOf(rowLetter)] + 499600
    }

    return UtmCoordinate(longitudeZone, latitudeZoneLetter, utmEasting.toDouble(), utmNorthing)
}