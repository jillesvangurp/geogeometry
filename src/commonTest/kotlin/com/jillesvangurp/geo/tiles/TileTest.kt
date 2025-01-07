package com.jillesvangurp.geo.tiles

import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

class TileTest {

    @Test
    fun randomizedCoordinateTests() {
        val zoomLevels = 0..20 // Typical zoom levels for tiles

        assertSoftly {
            repeat(10000) { // Number of random tests
                val lat = Random.nextDouble(
                    -85.05112878,
                    85.05112878,
                ) // Latitudes within the Web Mercator bounds
                val lon = Random.nextDouble(-180.0, 180.0) // Longitudes within the globe
                val zoom = zoomLevels.random()

                withClue("($lat, $lon) @ $zoom") {
                    val tile = Tile.deg2num(lat, lon, zoom)

                    val topLeft =
                        Tile.topLeft(tile.x, tile.y, zoom)

                    withClue("topleft should be north and west of original ") {
                        // top left should be north
                        topLeft.latitude shouldBeGreaterThanOrEqual lat
                        // top left should be west
                        topLeft.longitude shouldBeLessThanOrEqual lon
                    }

                    val recalculatedTile =
                        Tile.deg2num(topLeft.latitude, topLeft.longitude, zoom)

                    // Validate that the recalculated tile matches the original tile
                    withClue("x should be same") {
                        recalculatedTile.x shouldBe tile.x
                    }
                    withClue("y should be same") {
                        // rounding errors can cause this to fall into the neighboring tile
                        recalculatedTile.y shouldBeIn  arrayOf(tile.y, tile.y-1)
                    }
                }
            }
        }
    }

    @Test
    fun shouldConvertCorrectly() {
        data class TestCase(
            val zoom: Int,
            val x: Int,
            val y: Int,
            val lat: Double,
            val lon: Double,
        )

        // manually verified on openstreetmap

        val testCases = listOf(
            TestCase(zoom = 13, x = 4399, y = 2687, lat = 52.49867, lon = 13.34169),
            TestCase(14, 8802, 5373,52.5200, 13.4050),
            TestCase(zoom = 18, x = 232797, y = 103246, lat = 35.659062,lon=139.698054),
        )
        assertSoftly {
            testCases.forEach {t ->
                withClue("$t -> https://www.openstreetmap.org/#map=${t.zoom}/${t.lat}/${t.lon} https://tile.openstreetmap.org/${t.zoom}/${t.x}/${t.y}.png") {
                    val (zoom,x,y,lat,lon) = t
                    val topLeft = Tile.topLeft(x, y, zoom)
                    withClue("topLeft of tile should be north and west of actual" ) {
                        topLeft.latitude shouldBeGreaterThanOrEqual t.lat
                        topLeft.longitude shouldBeLessThanOrEqual t.lon
                    }
                    val xy = Tile.deg2num(lat, lon, zoom)
                    withClue("x") {
                        xy.x shouldBe x
                    }
                    withClue("y") {
                        xy.y shouldBe y
                    }
                }
            }
        }
    }

    @Test
    fun testTileNeighborsAtGeneralPositions() {
        val tile = Tile(10, 10, 5)

        tile.east shouldBe Tile(11, 10, 5)
        tile.west shouldBe Tile(9, 10, 5)
        tile.north shouldBe Tile(10, 9, 5)
        tile.south shouldBe Tile(10, 11, 5)
        tile.northWest shouldBe Tile(9, 9, 5)
        tile.southWest shouldBe Tile(9, 11, 5)
        tile.southEast shouldBe Tile(11, 11, 5)
        tile.northEast shouldBe Tile(11, 9, 5)
    }

    @Test
    fun testTileNeighborsAtMinBounds() {
        val tile = Tile(0, 0, 5)

        tile.east shouldBe Tile(1, 0, 5)
        tile.west shouldBe Tile(31, 0, 5) // Wrap around longitude
        tile.north shouldBe Tile(0, 0, 5) // Latitude already at min
        tile.south shouldBe Tile(0, 1, 5)
        tile.northWest shouldBe Tile(31, 0, 5) // Wrap around longitude
        tile.southWest shouldBe Tile(31, 1, 5) // Wrap around longitude
        tile.southEast shouldBe Tile(1, 1, 5)
        tile.northEast shouldBe Tile(1, 0, 5)
    }

    @Test
    fun testTileNeighborsAtMaxBounds() {
        val zoom = 5
        val maxTiles = (1 shl zoom) - 1
        val tile = Tile(maxTiles, maxTiles, zoom)

        tile.east shouldBe Tile(0, maxTiles, zoom) // Wrap around longitude
        tile.west shouldBe Tile(maxTiles - 1, maxTiles, zoom)
        tile.north shouldBe Tile(maxTiles, maxTiles - 1, zoom)
        tile.south shouldBe Tile(maxTiles, maxTiles, zoom) // Latitude already at max
        tile.northWest shouldBe Tile(maxTiles - 1, maxTiles - 1, zoom)
        tile.southWest shouldBe Tile(maxTiles - 1, maxTiles, zoom)
        tile.southEast shouldBe Tile(0, maxTiles, zoom) // Wrap around longitude
        tile.northEast shouldBe Tile(0, maxTiles - 1, zoom) // Wrap around longitude
    }

    @Test
    fun testTileNeighborsAtZoomZero() {
        val tile = Tile(0, 0, 0)

        tile.east shouldBe tile
        tile.west shouldBe tile
        tile.north shouldBe tile
        tile.south shouldBe tile
        tile.northWest shouldBe tile
        tile.southWest shouldBe tile
        tile.southEast shouldBe tile
        tile.northEast shouldBe tile
    }
}
