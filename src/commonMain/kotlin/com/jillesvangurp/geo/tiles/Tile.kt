package com.jillesvangurp.geo.tiles

import com.jillesvangurp.geo.tiles.Tile.Companion.MAX_ZOOM
import com.jillesvangurp.geo.tiles.Tile.Companion.coordinateToTile
import com.jillesvangurp.geojson.BoundingBox
import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
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
    val maxXY by lazy {
        1 shl zoom
    }

    init {
        require(zoom in 0..MAX_ZOOM) { "zoom should be between 0 and 22" }
        require(x in 0..maxXY) { "x must be between 0 and $maxXY at $zoom" }
        require(y in 0..maxXY) { "y must be between 0 and $maxXY at $zoom" }
    }

    companion object {
        const val MAX_ZOOM = 22
        const val MIN_LATITUDE = -85.05112878
        const val MAX_LATITUDE = 85.05112878

        /**
         * Returns the topLeft corner of the tile.
         */
        fun topLeft(x: Int, y: Int, zoom: Int): PointCoordinates {
            // n is the number of x and y coordinates at a zoom level
            // The shl operation (1 shl zoom) shifts the integer 1 to the left by zoom bits,
            // which is equivalent to calculating  2^zoom
            require(zoom in 0..MAX_ZOOM) { "zoom should be between 0 and 22" }
            val maxCoords = 1 shl zoom
            require(x in 0..maxCoords) { "x must be between 0 and $maxCoords at $zoom" }
            require(y in 0..maxCoords) { "y must be between 0 and $maxCoords at $zoom" }
            val lon = x.toDouble() / maxCoords * 360.0 - 180.0
            val lat = atan(sinh(PI * (1 - 2 * y.toDouble() / maxCoords))).toDegrees()
            return doubleArrayOf(lon, lat)
        }

        /**
         * Return the tile that contains the coordinate at the specified [zoom]
         */
        fun coordinateToTile(
            lat: Double,
            lon: Double,
            zoom: Int
        ): Tile {
            require(lat in -90.0..90.0) { "Latitude must be in range [-90, 90], but was $lat" }
            require(lon in -180.0..180.0) { "Longitude must be in range [-180, 180], but was $lon" }
            require(zoom in 0..MAX_ZOOM) { "Zoom level must be in range [0, $MAX_ZOOM], but was $zoom" }

            // Tiles don't actually work near the poles. Correct behavior is to clamp into these ranges but not fail
            val clampedLat = lat.coerceIn(MIN_LATITUDE, MAX_LATITUDE)
            val n = 1 shl zoom
            val x = ((lon + 180.0) / 360.0 * n).toInt()
            val y =
                ((1.0 - ln(tan(toRadians(clampedLat)) + 1 / cos(toRadians(clampedLat))) / PI) / 2.0 * n).toInt()
            return Tile(x = x, y = y, zoom = zoom)
        }

        @Deprecated("use coordinateToTile", ReplaceWith("coordinateToTile(lat,lon,zoom)"))
        fun deg2num(
            lat: Double,
            lon: Double,
            zoom: Int
        ) = coordinateToTile(lat, lon, zoom)

        @Deprecated("use coordinateToTile", ReplaceWith("coordinateToTile(p.latitude,p.longitude,zoom)"))
        fun deg2num(p: PointCoordinates, zoom: Int) = coordinateToTile(p.latitude, p.longitude, zoom)

    }
}

val Tile.topLeft get() = Tile.topLeft(x, y, zoom)
val Tile.topRight: PointCoordinates
    get() = Tile.topLeft((x + 1) % maxXY, y, zoom)

val Tile.bottomLeft: PointCoordinates
    get() = Tile.topLeft(x, (y + 1) % maxXY, zoom)

val Tile.bottomRight: PointCoordinates
    get() = Tile.topLeft((x + 1) % maxXY, (y + 1) % maxXY, zoom)

val Tile.bbox: BoundingBox
    get() {
        return doubleArrayOf(topLeft.longitude, bottomRight.latitude, bottomRight.longitude, topLeft.latitude)
    }

val Tile.east: Tile
    get() = Tile((x + 1) % maxXY, y, zoom)

val Tile.west: Tile
    get() = Tile((x - 1 + maxXY) % maxXY, y, zoom)

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

fun Tile.parentTiles(): List<Tile> {
    if (zoom == 0) return emptyList()
    val parentTiles = mutableListOf<Tile>()

    var currentTile = this
    for (z in zoom - 1 downTo 0) {
        val parentX = currentTile.x / 2
        val parentY = currentTile.y / 2
        currentTile = Tile(parentX, parentY, z)
        parentTiles.add(currentTile)
    }

    return parentTiles
}

fun PointCoordinates.tiles() =
    coordinateToTile(lat = this.latitude, lon = this.longitude, zoom = MAX_ZOOM).let { listOf(it) + it.parentTiles() }