package com.jillesvangurp.geo

import kotlin.math.roundToInt


private val eastingLetters = "ABCDEFGHJKLMNPQRSTUVWXYZ" // Excludes I and O
private val northingLetters = "ABCDEFGHJKLMNPQRSTUV"    // Excludes I and O

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

fun UtmCoordinate.convertUTMToMGRS(): MgrsCoordinate {
    val eastingLetter = getEastingLetter(longitudeZone, easting.toInt())
    val northingLetter = getNorthingLetter(northing.toInt(), latitudeZoneLetter)

    val eastingValue = (easting % 100000).roundToInt()
    val northingValue = (northing % 100000).roundToInt()

    return MgrsCoordinate(
        longitudeZone,
        latitudeZoneLetter,
        eastingLetter,
        northingLetter,
        eastingValue.toString().padStart(5, '0').toInt(),
        northingValue.toString().padStart(5, '0').toInt()
    )
}

private fun getEastingLetter(zoneNumber: Int, easting: Int): Char {
    val gridEasting = (easting / 100000)
    val index = (gridEasting - 1 + (zoneNumber - 1) * 8) % eastingLetters.length
    return eastingLetters[index]
}

//private fun getNorthingLetter(zoneNumber: Int, northing: Int): Char {
//    val gridNorthing = (northing / 100000)
//    val index = (gridNorthing - 1 + (zoneNumber - 1) * 2) % northingLetters.length
//    return northingLetters[index]
//}

private fun getNorthingLetter(northing: Int, latitudeZoneLetter: Char): Char {
    // Northing value should be adjusted for the false northing if in the southern hemisphere.
    val adjustedNorthing = if (latitudeZoneLetter < 'N') northing + 10000000 else northing
    // The northingLetters string starts with 'A' for 0m N, so no need to subtract 1 from the index.
    val index = (adjustedNorthing / 100000) % northingLetters.length
    return northingLetters[index]
}


fun convertMGRSToUTM(
    zoneNumber: Int,
    zoneLetter: Char,
    gridSquare: String,
    easting: Int,
    northing: Int
): UtmCoordinate {
    val eastingLetter = gridSquare[0]
    val northingLetter = gridSquare[1]

    val eastingOffset = getEastingOffset(eastingLetter, zoneNumber)
    val northingOffset = getNorthingOffset(northingLetter, zoneNumber, zoneLetter)

    val fullEasting = eastingOffset * 100000 + easting
    val fullNorthing = northingOffset * 100000 + northing

    return UtmCoordinate(zoneNumber, zoneLetter, fullEasting.toDouble(), fullNorthing.toDouble())
}

private fun getEastingOffset(eastingLetter: Char, zoneNumber: Int): Int {
    val eastingIndex = eastingLetters.indexOf(eastingLetter)
    return (eastingIndex + 1 - (zoneNumber - 1) * 8 + eastingLetters.length) % eastingLetters.length
}

private fun getNorthingOffset(northingLetter: Char, zoneNumber: Int, zoneLetter: Char): Int {
    var northingIndex = northingLetters.indexOf(northingLetter)
    northingIndex = (northingIndex + 1 - (zoneNumber - 1) * 2 + northingLetters.length) % northingLetters.length

    // Adjust for the latitude band
    val northingBase = "CDEFGHJKLMNPQRSTUVWX".indexOf(zoneLetter.uppercaseChar()) * 8
    return (northingBase + northingIndex) % northingLetters.length
}



