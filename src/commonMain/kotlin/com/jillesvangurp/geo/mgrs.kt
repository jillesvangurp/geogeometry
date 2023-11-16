package com.jillesvangurp.geo

import kotlin.math.floor
import kotlin.math.roundToInt

//private val eastingLetters = "ABCDEFGHJKLMNPQRSTUVWXYZ" // Excludes I and O
//private val northingLetters = "ABCDEFGHJKLMNPQRSTUV"    // Excludes I and O

data class MgrsCoordinate(
    val longitudeZone: Int,
    val latitudeZoneLetter: Char,
    val eastingLetter: Char,
    val northingLetter: Char,
    val easting: Int,
    val northing: Int
) {
    override fun toString(): String {
        return "$longitudeZone$latitudeZoneLetter $eastingLetter$northingLetter $easting $northing"
    }
}

fun Int.setForZone(): Int {
    return when(this%6) {
        0 ->  6
        1 ->  1
        2 ->  2
        3 ->  3
        4 ->  4
        5 ->  5
        else -> error("should not happen")
    }
}

private const val BLOCK_SIZE=100000

fun UtmCoordinate.lookupGridLetters(): Pair<Char, Char> {
    var row=1
    var n = northing.roundToInt()
    while(n >= BLOCK_SIZE) {
        n -= BLOCK_SIZE
        row++
    }
    row %= 20

    var col = 0
    var e = easting.roundToInt()
    while(e>= BLOCK_SIZE) {
        e -= BLOCK_SIZE
        col++
    }
    col %= 8

    val set = longitudeZone.setForZone()

    val l1Set = when(set) {
        1->"ABCDEFGH"
        2->"JKLMNPQR"
        3->"STUVWXYZ"
        4->"ABCDEFGH"
        5->"JKLMNPQR"
        6->"STUVWXYZ"
        else -> error("should not happen")

    }
    val l2Set = if(set%2==0) "FGHJKLMNPQRSTUVABCDE" else "ABCDEFGHJKLMNPQRSTUV"

    val actualCol = if(col==0) 7 else col-1
    val actualRow = if(row==0) 19 else row-1
    return l1Set[actualCol] to l2Set[actualRow]
}

fun UtmCoordinate.convertUTMToMGRS(): MgrsCoordinate {
    val (l1,l2) = lookupGridLetters()

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

/**
 * Half working implementation based of chat gpt. Don't use this.
 *
 * TODO find better implementation
 */
//fun convertMGRSToUTM(
//    zoneNumber: Int,
//    zoneLetter: Char,
//    gridSquare: String,
//    easting: Int,
//    northing: Int
//): UtmCoordinate {
//    val eastingLetter = gridSquare[0]
//    val northingLetter = gridSquare[1]
//
//    val eastingOffset = getEastingOffset(eastingLetter, zoneNumber)
//    val northingOffset = getNorthingOffset(northingLetter, zoneNumber, zoneLetter)
//
//    val fullEasting = eastingOffset * 100000 + easting
//    val fullNorthing = northingOffset * 100000 + northing
//
//    return UtmCoordinate(zoneNumber, zoneLetter, fullEasting.toDouble(), fullNorthing.toDouble())
//}

//private fun getEastingOffset(eastingLetter: Char, zoneNumber: Int): Int {
//    val eastingIndex = eastingLetters.indexOf(eastingLetter)
//    return (eastingIndex + 1 - (zoneNumber - 1) * 8 + eastingLetters.length) % eastingLetters.length
//}
//
//private fun getNorthingOffset(northingLetter: Char, zoneNumber: Int, zoneLetter: Char): Int {
//    var northingIndex = northingLetters.indexOf(northingLetter)
//    northingIndex = (northingIndex + 1 - (zoneNumber - 1) * 2 + northingLetters.length) % northingLetters.length
//
//    // Adjust for the latitude band
//    val northingBase = "CDEFGHJKLMNPQRSTUVWX".indexOf(zoneLetter.uppercaseChar()) * 8
//    return (northingBase + northingIndex) % northingLetters.length
//}



