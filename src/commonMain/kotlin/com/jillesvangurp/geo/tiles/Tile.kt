package com.jillesvangurp.geo.tiles

import com.jillesvangurp.geojson.BoundingBox
import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan
import kotlinx.serialization.Serializable

private fun Double.toDegrees() = this * 180.0 / PI
private fun toRadians(degrees: Double) = degrees * PI / 180.0

/**
 * Representation of Google and OSM style map tile
 *
 * zoom levels are from 0-22
 * each zoom level you have 2^zoom x and y tiles (4.1Mx4.1M at max zoom level)
 */
@Serializable
data class Tile(val x: Int, val y: Int, val zoom: Int) {
    companion object {
        /**
         * Returns the topLeft corner of the tile.
         */
        fun topLeft(x: Int, y: Int, zoom: Int): PointCoordinates {
            // n is the number of x and y coordinates at a zoom level
            // The shl operation (1 shl zoom) shifts the integer 1 to the left by zoom bits,
            // which is equivalent to calculating  2^zoom
            val maxCoords = 1 shl zoom
            val lon = x.toDouble() / maxCoords * 360.0 - 180.0
            val lat = atan(sinh(PI * (1 - 2 * y.toDouble() / maxCoords))).toDegrees()
            return doubleArrayOf(lon,lat)
        }

        /**
         * Calculate the x,y coordinate at the zoom level and return that as a pair.
         */
        fun deg2num(
            lat: Double,
            lon: Double,
            zoom: Int
        ): Tile {
            // FIXME add range checks on lat, lon, and zoom but do it in a way that doesn't impact performance
            val n = 1 shl zoom
            val x = ((lon + 180.0) / 360.0 * n).toInt()
            val y =
                ((1.0 - ln(tan(toRadians(lat)) + 1 / cos(toRadians(lat))) / PI) / 2.0 * n).toInt()
            return Tile(x = x, y = y, zoom = zoom)
        }

        fun deg2num(p: PointCoordinates, zoom: Int) = deg2num(p.latitude, p.longitude, zoom)

    }
}

val Tile.topLeft get() = Tile.topLeft(x,y,zoom)

val Tile.bbox: BoundingBox
    get() {
        val bottomRight = southWest.topLeft
        return doubleArrayOf(topLeft.longitude, bottomRight.latitude, bottomRight.longitude, topLeft.latitude)
    }

val Tile.east: Tile
    get() = Tile((x + 1) % (1 shl zoom), y, zoom)

val Tile.west: Tile
    get() = Tile((x - 1 + (1 shl zoom)) % (1 shl zoom), y, zoom)

val Tile.north: Tile
    get() {
        return if (y > 0) Tile(x, y - 1, zoom) else Tile(x, 0, zoom)
    }

val Tile.south: Tile
    get() {
        val maxTiles = 1 shl zoom
        return if (y < maxTiles - 1) Tile(x, y + 1, zoom) else Tile(x, maxTiles - 1, zoom)
    }

val Tile.northWest: Tile
    get() = north.west

val Tile.southWest: Tile
    get() = south.west

val Tile.southEast: Tile
    get() = south.east

val Tile.northEast: Tile
    get() = north.east
