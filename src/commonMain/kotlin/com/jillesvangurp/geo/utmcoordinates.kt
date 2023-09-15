package com.jillesvangurp.geo

import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.math.*


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

fun String.parseUTM(): UTM {
    val parts = split("\\s+".toRegex())
    val zone = parts[0].toInt()
    val letter = parts[1].uppercase()[0]
    val easting = parts[2].toDouble()
    val northing = parts[3].toDouble()
    return UTM(zone = zone, letter = letter, easting = easting, northing = northing)
}

val String.utmAsWgs84 get() = parseUTM().toWgs84()

fun PointCoordinates.toUTM(): UTM {
    val zone = floor(longitude / 6 + 31).toInt()
    val letter =
        if (latitude < -72) 'C' else if (latitude < -64) 'D' else if (latitude < -56) 'E' else if (latitude < -48) 'F' else if (latitude < -40) 'G' else if (latitude < -32) 'H' else if (latitude < -24) 'J' else if (latitude < -16) 'K' else if (latitude < -8) 'L' else if (latitude < 0) 'M' else if (latitude < 8) 'N' else if (latitude < 16) 'P' else if (latitude < 24) 'Q' else if (latitude < 32) 'R' else if (latitude < 40) 'S' else if (latitude < 48) 'T' else if (latitude < 56) 'U' else if (latitude < 64) 'V' else if (latitude < 72) 'W' else 'X'
    var easting = 0.5 * ln(
        (1 + cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180)) / (1 - cos(
            latitude * PI / 180
        ) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180))
    ) * 0.9996 * 6399593.62 / (1 + 0.0820944379.pow(2.0) * cos(latitude * PI / 180).pow(2.0)).pow(0.5) * (1 + 0.0820944379.pow(
        2.0
    ) / 2 * (0.5 * ln(
        (1 + cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180)) / (1 - cos(
            latitude * PI / 180
        ) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180))
    )).pow(2.0) * cos(latitude * PI / 180).pow(2.0) / 3) + 500000
    easting = round(easting * 100) * 0.01
    var northing =
        (atan(tan(latitude * PI / 180) / cos(longitude * PI / 180 - (6 * zone - 183) * PI / 180)) - latitude * PI / 180) * 0.9996 * 6399593.625 / sqrt(
            1 + 0.006739496742 * cos(latitude * PI / 180).pow(2.0)
        ) * (1 + 0.006739496742 / 2 * (0.5 * ln(
            (1 + cos(latitude * PI / 180) * sin(
                longitude * PI / 180 - (6 * zone - 183) * PI / 180
            )) / (1 - cos(latitude * PI / 180) * sin(longitude * PI / 180 - (6 * zone - 183) * PI / 180))
        )).pow(2.0) * cos(latitude * PI / 180).pow(2.0)) + 0.9996 * 6399593.625 * (latitude * PI / 180 - 0.005054622556 * (latitude * PI / 180 + sin(
            2 * latitude * PI / 180
        ) / 2) + 4.258201531e-05 * (3 * (latitude * PI / 180 + sin(2 * latitude * PI / 180) / 2) + sin(
            2 * latitude * PI / 180
        ) * cos(latitude * PI / 180).pow(2.0)) / 4 - 1.674057895e-07 * (5 * (3 * (latitude * PI / 180 + sin(
            2 * latitude * PI / 180
        ) / 2) + sin(2 * latitude * PI / 180) * cos(latitude * PI / 180).pow(2.0)) / 4 + sin(
            2 * latitude * PI / 180
        ) * cos(latitude * PI / 180).pow(2.0) * cos(latitude * PI / 180).pow(2.0)) / 3)
    if (letter < 'M') northing = northing + 10000000
    northing = round(northing * 100) * 0.01

    return UTM(
        zone = zone, letter = letter, easting = easting, northing = northing
    )
}

val PointCoordinates.format get() = run {
    val ns = if (latitude < 0) 'S' else 'N'
    val ew = if (longitude < 0) 'W' else 'E'
    "${abs(latitude)}$ns ${abs(longitude)}$ew"
}

fun WGS84.toUTM() = doubleArrayOf(longitude, latitude).toUTM()

class WGS84(
    val latitude: Double = 0.0, val longitude: Double = 0.0
) {

    override fun toString(): String {
        val ns = if (latitude < 0) 'S' else 'N'
        val ew = if (longitude < 0) 'W' else 'E'
        return "${abs(latitude)}$ns ${abs(longitude)}$ew"
    }
}

fun UTM.toWgs84(): PointCoordinates {
    val north: Double = if (letter > 'M') {
        northing
    } else {
        northing - 10000000
    }
    var latitude =
        (north / 6366197.724 / 0.9996 + (1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0) - 0.006739496742 * sin(
            north / 6366197.724 / 0.9996
        ) * cos(north / 6366197.724 / 0.9996) * (atan(
            cos(
                atan(
                    (exp(
                        (easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                        )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(
                                north / 6366197.724 / 0.9996
                            ).pow(2.0)
                        ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0) / 3)
                    ) - exp(
                        -(easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                        )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(
                                north / 6366197.724 / 0.9996
                            ).pow(2.0)
                        ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0) / 3)
                    )) / 2 / cos(
                        (north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) / 2) + (0.006739496742 * 3 / 4).pow(2.0) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 - (0.006739496742 * 3 / 4).pow(
                            3.0
                        ) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) * cos(north / 6366197.724 / 0.9996).pow(2.0) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 3)) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                        )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(
                                north / 6366197.724 / 0.9996
                            ).pow(2.0)
                        ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0)) + north / 6366197.724 / 0.9996
                    )
                )
            ) * tan(
                (north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) / 2) + (0.006739496742 * 3 / 4).pow(2.0) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 - (0.006739496742 * 3 / 4).pow(
                    3.0
                ) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) * cos(north / 6366197.724 / 0.9996).pow(2.0) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 3)) / (0.9996 * 6399593.625 / sqrt(
                    1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                    1 + 0.006739496742 * cos(
                        north / 6366197.724 / 0.9996
                    ).pow(2.0)
                ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0)) + north / 6366197.724 / 0.9996
            )
        ) - north / 6366197.724 / 0.9996) * 3 / 2) * (atan(
            cos(
                atan(
                    (exp(
                        (easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                        )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(
                                north / 6366197.724 / 0.9996
                            ).pow(2.0)
                        ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0) / 3)
                    ) - exp(
                        -(easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                        )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(
                                north / 6366197.724 / 0.9996
                            ).pow(2.0)
                        ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0) / 3)
                    )) / 2 / cos(
                        (north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) / 2) + (0.006739496742 * 3 / 4).pow(2.0) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 - (0.006739496742 * 3 / 4).pow(
                            3.0
                        ) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 + sin(
                            2 * north / 6366197.724 / 0.9996
                        ) * cos(north / 6366197.724 / 0.9996).pow(2.0) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 3)) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                        )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                            1 + 0.006739496742 * cos(
                                north / 6366197.724 / 0.9996
                            ).pow(2.0)
                        ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0)) + north / 6366197.724 / 0.9996
                    )
                )
            ) * tan(
                (north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) / 2) + (0.006739496742 * 3 / 4).pow(2.0) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 - (0.006739496742 * 3 / 4).pow(
                    3.0
                ) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 + sin(
                    2 * north / 6366197.724 / 0.9996
                ) * cos(north / 6366197.724 / 0.9996).pow(2.0) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 3)) / (0.9996 * 6399593.625 / sqrt(
                    1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
                )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                    1 + 0.006739496742 * cos(
                        north / 6366197.724 / 0.9996
                    ).pow(2.0)
                ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0)) + north / 6366197.724 / 0.9996
            )
        ) - north / 6366197.724 / 0.9996)) * 180 / PI
    latitude = round(latitude * 10000000).toDouble()
    latitude /= 10000000
    var longitude = atan(
        (exp(
            (easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(
                    2.0
                )
            )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
            ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0) / 3)
        ) - exp(
            -(easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
            )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                1 + 0.006739496742 * cos(
                    north / 6366197.724 / 0.9996
                ).pow(2.0)
            ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0) / 3)
        )) / 2 / cos(
            (north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996 - 0.006739496742 * 3 / 4 * (north / 6366197.724 / 0.9996 + sin(
                2 * north / 6366197.724 / 0.9996
            ) / 2) + (0.006739496742 * 3 / 4).pow(2.0) * 5 / 3 * (3 * (north / 6366197.724 / 0.9996 + sin(
                2 * north / 6366197.724 / 0.9996
            ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 - (0.006739496742 * 3 / 4).pow(
                3.0
            ) * 35 / 27 * (5 * (3 * (north / 6366197.724 / 0.9996 + sin(
                2 * north / 6366197.724 / 0.9996
            ) / 2) + sin(2 * north / 6366197.724 / 0.9996) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 4 + sin(
                2 * north / 6366197.724 / 0.9996
            ) * cos(north / 6366197.724 / 0.9996).pow(2.0) * cos(north / 6366197.724 / 0.9996).pow(2.0)) / 3)) / (0.9996 * 6399593.625 / sqrt(
                1 + 0.006739496742 * cos(north / 6366197.724 / 0.9996).pow(2.0)
            )) * (1 - 0.006739496742 * ((easting - 500000) / (0.9996 * 6399593.625 / sqrt(
                1 + 0.006739496742 * cos(
                    north / 6366197.724 / 0.9996
                ).pow(2.0)
            ))).pow(2.0) / 2 * cos(north / 6366197.724 / 0.9996).pow(2.0)) + north / 6366197.724 / 0.9996
        )
    ) * 180 / PI + zone * 6 - 183
    longitude = round(longitude * 10000000).toDouble()
    longitude /= 10000000

    return doubleArrayOf(longitude, latitude)
}
