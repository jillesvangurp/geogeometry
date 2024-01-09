package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geogeometry.bergstr16Berlin
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldInclude
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlin.test.Test

class GeojsonKtTest {

    @Test
    fun shouldCalculateZoomLevel() {
        val meters = 75.0

        val zl1 = GeoGeometry.bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            meters,
            meters
        ).zoomLevel()

        val zl2 = GeoGeometry.bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            meters * 16,
            meters * 16
        ).zoomLevel()

        println(zl1)
        println(zl2)
        // 2  ^ 4  = 16 x the size means 4 zoom levels difference
        zl1 - zl2 shouldBe 4
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun ByteArray.toHex(): String = asUByteArray().joinToString("") { it.toString(radix = 16).padStart(2, '0') }

    @Test
    fun shouldTile() {

        val bbox = GeoGeometry.bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            100.0 * 16,
            100.0 * 16
        )
        val cells = GeoGeometry.calculateTileBboxesForBoundingBox(bbox)
        val collection =
            FeatureCollection(cells.map { it.polygon() }.map { it.asFeature() } + listOf(bbox.polygon().asFeature()))
        val json = collection.toString()
        val parsed = Json.decodeFromString(FeatureCollection.serializer(), json)
        parsed.features shouldContainInOrder collection.features
        parsed shouldBe collection

//
//        println(parsed)
//        val cbor = Cbor.encodeToByteArray(FeatureCollection.serializer(),collection)
//        println(cbor.decodeToString())
//        println(cbor.toHex())
//        val decoded = Cbor.decodeFromByteArray(FeatureCollection.serializer(),cbor)
//        println(decoded)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun cbor() {
        val p = Geometry.Point(coordinates = doubleArrayOf(1.0,1.0))
        val cb = Cbor {
            encodeDefaults = true
        }
        val bytes = cb.encodeToByteArray(Geometry.serializer(),p)
        println(bytes.decodeToString())
        println(bytes.toHex())
        val decoded = cb.decodeFromByteArray(Geometry.serializer(), bytes)
        println(decoded)
        decoded shouldBe p
    }

    @Test
    fun shouldCoverBerlinWithGeohashes() {
        val berlin = DEFAULT_JSON.decodeFromString(Geometry.serializer(), berlinGeoJson) as Geometry.MultiPolygon

        val hashes = GeoHashUtils.geoHashesForMultiPolygon(
            coordinates = berlin.coordinates ?: throw IllegalArgumentException("coordinates missing"),
            includePartial = true,
            maxLength = 6
        )
        FeatureCollection.fromGeoHashes(hashes)
        val json = DEFAULT_JSON.encodeToString(
            FeatureCollection.serializer(),
            //hashesCollection +
            FeatureCollection(listOf(berlin.asFeature()))
        )
        // make sure we serialize these fields; they are required.
        json shouldInclude """"type":"Feature""""
        json shouldInclude """"type":"FeatureCollection""""
        // you can copy paste this to geojson.io
        println(
            json
        )
    }

    @Test
    fun equalsTestForGeometry() {
        val berlin = DEFAULT_JSON.decodeFromString(Geometry.serializer(), berlinGeoJson) as Geometry.MultiPolygon

        berlin.shouldBe(berlin)
        berlin.shouldBe(berlin.copy())
        berlin shouldNotBe berlin.copy(coordinates = null)
        berlin.copy(coordinates = null) shouldNotBe berlin
        val reversed = berlin.coordinates?.copyOf()
        reversed?.reverse()
        berlin.copy(coordinates = reversed) shouldNotBe berlin

        Geometry.Point(null) shouldBe Geometry.Point(null)
        Geometry.Point(doubleArrayOf(0.1, 0.1)) shouldNotBe Geometry.Point(null)
        Geometry.Point(null) shouldNotBe Geometry.Point(doubleArrayOf(0.1, 0.1))
        Geometry.Point(doubleArrayOf(0.1, 0.1)) shouldNotBe Geometry.Point(doubleArrayOf(0.9, 0.9))
        Geometry.Point(doubleArrayOf(0.1, 0.1)) shouldBe Geometry.Point(doubleArrayOf(0.1, 0.1))
    }

}
