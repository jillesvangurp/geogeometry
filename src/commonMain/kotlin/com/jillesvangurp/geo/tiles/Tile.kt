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
import kotlin.math.sinh
import kotlin.math.tan
import kotlinx.serialization.Serializable

private fun Double.toDegrees() = this * 180.0 / PI
private fun toRadians(degrees: Double) = degrees * PI / 180.0

/**
 * Representation of Google and OSM style map tile grid systems. Tiles like this are commonly used
 * for mapping. This Kotlin class makes it easy to work with tiles, convert to and from coordinates,
 * quad tree paths, calculate neighboring tiles, bounding boxes, etc.
 *
 * [zoom] levels are from 0-22
 * each zoom level you have 2^zoom [x] and [y] tiles (4.1Mx4.1M at max zoom level)
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

    override fun toString() = "$zoom/$x/$y"

    /**
     * Converts this tile to a QuadKey string with a base 4 representation of the quad tree path
     *
     * Tiles with the same quadkey prefix share the same parent tile.
     */
    fun toQuadKey(): String {
        val quadKey = StringBuilder()
        for (i in zoom downTo 1) {
            var digit = 0
            val mask = 1 shl (i - 1)
            if ((x and mask) != 0) digit += 1
            if ((y and mask) != 0) digit += 2
            quadKey.append(digit)
        }
        return quadKey.toString()
    }

    /**
     * Converts this tile to a compact Long representation of its QuadKey.
     *
     * More compact than the string because it works at the bit level.
     */
    fun toQuadKeyLong(): Long {
        var value = 0L
        for (digit in toQuadKey()) {
            value = (value shl 2) or ((digit - '0').toLong())
        }
        return value
    }

    val topLeft: PointCoordinates by lazy { topLeft(x = x, y = y, zoom = zoom) }

    val topRight: PointCoordinates by lazy { topLeft(x = (x + 1) % maxXY, y = y, zoom = zoom, fixLonLat = true) }

    val bottomLeft: PointCoordinates by lazy { topLeft(x = x, y = (y + 1) % maxXY, zoom = zoom) }

    val bottomRight: PointCoordinates by lazy { topLeft(
        x = (x + 1) % maxXY,
        y = (y + 1) % maxXY,
        zoom = zoom,
        fixLonLat = true
    ) }

    val bbox: BoundingBox by lazy {
        if(zoom>0) {
            doubleArrayOf(
                topLeft.longitude,
                bottomRight.latitude,
                bottomRight.longitude,
                topLeft.latitude
            )
        } else {
            doubleArrayOf(-180.0, MAX_LATITUDE,180.0, MIN_LATITUDE)
        }
    }

    val east: Tile by lazy { Tile((x + 1) % maxXY, y, zoom) }

    val west: Tile by lazy { Tile((x - 1 + maxXY) % maxXY, y, zoom) }

    val north: Tile by lazy {
        if (y > 0) Tile(x, y - 1, zoom) else Tile(x, 0, zoom)
    }

    val south: Tile by lazy {
        val maxTiles = 1 shl zoom
        if (y < maxTiles - 1) Tile(x, y + 1, zoom) else Tile(x, maxTiles - 1, zoom)
    }

    val northWest: Tile by lazy { north.west }

    val southWest: Tile by lazy { south.west }

    val southEast: Tile by lazy { south.east }

    val northEast: Tile by lazy { north.east }

    companion object {
        const val MAX_ZOOM = 22
        const val MIN_LATITUDE = -85.05112878
        const val MAX_LATITUDE = 85.05112878

        /**
         * Convert from a string like "22/2154259/1378425" to a Tile.
         */
        fun fromPath(path: String): Tile {
            val parts = path.split("/")
            require(parts.size == 3) { "Invalid path format. Expected format: zoom/x/y" }

            val zoom = parts[0].toIntOrNull()
            val x = parts[1].toIntOrNull()
            val y = parts[2].toIntOrNull()

            require(zoom != null && x != null && y != null) { "Invalid path components. Must be integers." }

            return Tile(x, y, zoom)
        }

        /**
         * Converts a QuadKey string back into a Tile.
         */
        fun fromQuadKey(quadKey: String): Tile {
            var x = 0
            var y = 0
            val zoom = quadKey.length
            for (i in 0 until zoom) {
                val bit = zoom - i - 1
                val mask = 1 shl bit
                when (quadKey[i]) {
                    '1' -> x = x or mask
                    '2' -> y = y or mask
                    '3' -> {
                        x = x or mask
                        y = y or mask
                    }
                }
            }
            return Tile(x, y, zoom)
        }

        /**
         * Converts a long representation of a QuadKey back into a Tile.
         */
        fun fromQuadKeyLong(value: Long, zoom: Int): Tile {
            val quadKey = StringBuilder()
            var v = value
            for (i in 0 until zoom) {
                val digit = (v and 3).toInt()  // Extract last 2 bits
                quadKey.append(digit)
                v = v shr 2  // Shift right to process next digit
            }
            return fromQuadKey(quadKey.reverse().toString())  // Reverse since digits are stored in reverse order
        }

        /**
         * Returns the topLeft corner of the tile. Use [fixLonLat] if you are
         * calculating the topleft of a tile that is North/East of the current one to
         * dodge issues with MIN/MAX latitude and the dateline
         */
        fun topLeft(x: Int, y: Int, zoom: Int, fixLonLat: Boolean=false): PointCoordinates {
            // n is the number of x and y coordinates at a zoom level
            // The shl operation (1 shl zoom) shifts the integer 1 to the left by zoom bits,
            // which is equivalent to calculating  2^zoom
            require(zoom in 0..MAX_ZOOM) { "zoom should be between 0 and 22" }
            val maxCoords = 1 shl zoom
            require(x in 0..maxCoords) { "x must be between 0 and $maxCoords at $zoom" }
            require(y in 0..maxCoords) { "y must be between 0 and $maxCoords at $zoom" }
            val lon = x.toDouble() / maxCoords * 360.0 - 180.0
            val lat =
                atan(sinh(PI * (1 - 2 * y.toDouble() / maxCoords)))
                    .toDegrees()
                    .coerceIn(MIN_LATITUDE, MAX_LATITUDE)


            return doubleArrayOf(
                if(fixLonLat && lon <= -180.0) 180.0 else lon,
                // nice little rounding error here calculating the bottom latitude
                if(fixLonLat && lat >= 85.051128) MIN_LATITUDE else lat
            )
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
            val x = (((lon + 180.0) / 360.0 * n) % n).toInt()
            val y =
                ((1.0 - ln(tan(toRadians(clampedLat)) + 1 / cos(toRadians(clampedLat))) / PI) / 2.0 * n).toInt()
            return Tile(x = x, y = y, zoom = zoom)
        }

        fun allTilesAt(zoom: Int): Sequence<Tile> {
            require(zoom in 0..MAX_ZOOM) { "Zoom level must be between 0 and $MAX_ZOOM." }
            val maxXY = 1 shl zoom
            return sequence {
                for (x in 0 until maxXY) {
                    for (y in 0 until maxXY) {
                        yield(Tile(x, y, zoom))
                    }
                }
            }
        }
    }
}

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

fun Tile.parentAtZoom(zoom: Int): Tile {
    require(zoom in 0 until this.zoom) { "Target zoom must be less than current zoom ($this.zoom)" }

    val scale = 1 shl (this.zoom - zoom)
    val parentX = this.x / scale
    val parentY = this.y / scale

    return Tile(parentX, parentY, zoom)
}

fun PointCoordinates.tiles() =
    coordinateToTile(lat = this.latitude, lon = this.longitude, zoom = MAX_ZOOM).let { listOf(it) + it.parentTiles() }