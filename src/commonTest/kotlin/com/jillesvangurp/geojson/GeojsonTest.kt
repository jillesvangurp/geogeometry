package com.jillesvangurp.geojson

import com.jillesvangurp.geogeometry.geometry.*
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geogeometry.bergstr16Berlin
import com.jillesvangurp.serializationext.DEFAULT_JSON
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import com.jillesvangurp.geojson.lonLat

class GeojsonKtTest {

    @Test
    fun shouldCalculateZoomLevel() {
        val meters = 75.0

        val zl1 = bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            meters,
            meters
        ).zoomLevel()

        val zl2 = bbox(
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

        val bbox = bbox(
            bergstr16Berlin.latitude, bergstr16Berlin.longitude,
            100.0 * 16,
            100.0 * 16
        )
        val cells = calculateTileBboxesForBoundingBox(bbox)
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
        val p = Geometry.Point(coordinates = lonLat(1.0, 1.0))
        val cb = Cbor {
            encodeDefaults = true
        }
        val bytes = cb.encodeToByteArray(Geometry.serializer(), p)
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
        Geometry.Point(lonLat(0.1, 0.1)) shouldNotBe Geometry.Point(null)
        Geometry.Point(null) shouldNotBe Geometry.Point(lonLat(0.1, 0.1))
        Geometry.Point(lonLat(0.1, 0.1)) shouldNotBe Geometry.Point(lonLat(0.9, 0.9))
        Geometry.Point(lonLat(0.1, 0.1)) shouldBe Geometry.Point(lonLat(0.1, 0.1))
    }

    @Test
    fun pointInsidePolygonShouldIntersect() {
        val point = Geometry.Point(lonLat(13.3889, 52.5170)) // Somewhere in Berlin
        val polygon = Geometry.Polygon(
            coordinates = arrayOf(
                arrayOf(
                    lonLat(13.387, 52.516),
                    lonLat(13.390, 52.516),
                    lonLat(13.390, 52.518),
                    lonLat(13.387, 52.518),
                    lonLat(13.387, 52.516)
                )
            )
        )
        point.intersects(polygon) shouldBe true
        polygon.intersects(point) shouldBe true
    }

    @Test
    fun disjointGeometriesShouldNotIntersect() {
        val point = Geometry.Point(lonLat(10.0, 10.0))
        val polygon = Geometry.Polygon(
            coordinates = arrayOf(
                arrayOf(
                    lonLat(0.0, 0.0),
                    lonLat(1.0, 0.0),
                    lonLat(1.0, 1.0),
                    lonLat(0.0, 1.0),
                    lonLat(0.0, 0.0)
                )
            )
        )
        point.intersects(polygon) shouldBe false
        polygon.intersects(point) shouldBe false
    }

    @Test
    fun touchingEdgesShouldIntersect() {
        val line1 = Geometry.LineString(
            coordinates = arrayOf(
                lonLat(0.0, 0.0),
                lonLat(1.0, 1.0)
            )
        )
        val line2 = Geometry.LineString(
            coordinates = arrayOf(
                lonLat(1.0, 1.0),
                lonLat(2.0, 2.0)
            )
        )
        line1.intersects(line2) shouldBe true
    }

    @Test
    fun overlappingLineSegmentsShouldIntersect() {
        val line1 = Geometry.LineString(
            coordinates = arrayOf(
                lonLat(0.0, 0.0),
                lonLat(2.0, 2.0)
            )
        )
        val line2 = Geometry.LineString(
            coordinates = arrayOf(
                lonLat(1.0, 1.0),
                lonLat(3.0, 3.0)
            )
        )
        line1.intersects(line2) shouldBe true
    }

    @Test
    fun antimeridianCrossingPolygonShouldIntersect() {
        // a simple 1°×1° box crossing the antimeridian between lon 179 and –179
        val polygon = Geometry.Polygon(
            coordinates = arrayOf(
                arrayOf(
                    lonLat(179.0, 0.0),
                    lonLat(-179.0, 0.0),
                    lonLat(-179.0, 1.0),
                    lonLat(179.0, 1.0),
                    lonLat(179.0, 0.0)
                )
            )
        )
        // points just inside on either side of the antimeridian
        val insideEast = Geometry.Point(lonLat(179.5, 0.5))
        val insideWest = Geometry.Point(lonLat(-179.5, 0.5))
        insideEast.intersects(polygon) shouldBe true
        insideWest.intersects(polygon) shouldBe true

        // a point well outside the crossing region
        val outside = Geometry.Point(lonLat(178.0, 0.5))
        outside.intersects(polygon) shouldBe false
    }

    @Test
    fun pointOnEdgeOrVertexShouldIntersect() {
        val square = Geometry.Polygon(
            arrayOf(
                arrayOf(
                    lonLat(0.0, 0.0),
                    lonLat(1.0, 0.0),
                    lonLat(1.0, 1.0),
                    lonLat(0.0, 1.0),
                    lonLat(0.0, 0.0)
                )
            )
        )
        val onEdge = Geometry.Point(lonLat(0.5, 0.0))
        val onVertex = Geometry.Point(lonLat(1.0, 1.0))
        onEdge.intersects(square) shouldBe true
        onVertex.intersects(square) shouldBe true
    }

    @Test
    fun polygonsTouchingAtVertexShouldIntersect() {
        val p1 = Geometry.Polygon(
            arrayOf(
                arrayOf(
                    lonLat(0.0, 0.0),
                    lonLat(1.0, 0.0),
                    lonLat(1.0, 1.0),
                    lonLat(0.0, 1.0),
                    lonLat(0.0, 0.0)
                )
            )
        )
        val p2 = Geometry.Polygon(
            arrayOf(
                arrayOf(
                    lonLat(1.0, 1.0),
                    lonLat(2.0, 1.0),
                    lonLat(2.0, 2.0),
                    lonLat(1.0, 2.0),
                    lonLat(1.0, 1.0)
                )
            )
        )
        p1.intersects(p2) shouldBe true
    }

    @Test
    fun zeroLengthLineShouldBehaveLikePoint() {
        val pt = Geometry.Point(lonLat(5.0, 5.0))
        val zeroLine = Geometry.LineString(arrayOf(lonLat(5.0, 5.0), lonLat(5.0, 5.0)))
        zeroLine.intersects(pt) shouldBe true
        zeroLine.intersects(
            Geometry.Polygon(
                arrayOf(
                    arrayOf(
                        lonLat(0.0, 0.0),
                        lonLat(10.0, 0.0),
                        lonLat(10.0, 10.0),
                        lonLat(0.0, 10.0),
                        lonLat(0.0, 0.0)
                    )
                )
            )
        ) shouldBe true
    }

    @Test
    fun multiGeometriesIntersectIfAnyMemberDoes() {
        val multiPts = Geometry.MultiPoint(
            arrayOf(
                lonLat(0.0, 0.0),
                lonLat(2.0, 2.0)
            )
        )
        val tri = Geometry.Polygon(
            arrayOf(
                arrayOf(
                    lonLat(1.0, 1.0),
                    lonLat(3.0, 1.0),
                    lonLat(2.0, 3.0),
                    lonLat(1.0, 1.0)
                )
            )
        )
        multiPts.intersects(tri) shouldBe true  // because (2,2) is inside
    }

    @Test
    fun geometryCollectionRespectsAnyIntersect() {
        val coll = Geometry.GeometryCollection(
            arrayOf(
                Geometry.Point(lonLat(10.0, 10.0)),
                Geometry.LineString(arrayOf(lonLat(0.0, 0.0), lonLat(1.0, 1.0)))
            )
        )
        val bigPoly = Geometry.Polygon(
            arrayOf(
                arrayOf(
                    lonLat(-1.0, -1.0),
                    lonLat(2.0, -1.0),
                    lonLat(2.0, 2.0),
                    lonLat(-1.0, 2.0),
                    lonLat(-1.0, -1.0)
                )
            )
        )
        coll.intersects(bigPoly) shouldBe true
    }

    @Test
    fun antimeridianLineCrossingShouldIntersectMultiPolygon() {
        val mp = Geometry.MultiPolygon(
            arrayOf(
                arrayOf(
                    arrayOf(
                        lonLat(179.0, 0.0), lonLat(-179.0, 0.0),
                        lonLat(-179.0, 1.0), lonLat(179.0, 1.0),
                        lonLat(179.0, 0.0)
                    )
                )
            )
        )
        val line = Geometry.LineString(
            arrayOf(
                lonLat(178.0, 0.5),
                lonLat(-178.0, 0.5)
            )
        )
        line.intersects(mp) shouldBe true
    }

    @Test
    fun nestedCircleShouldIntersect() {
        val c1 = circle2polygon(50, 52.0, 13.0, 10.0).asGeometry
        val c2 = circle2polygon(50, 52.0, 13.0, 5.0).asGeometry

        c1.intersects(c2) shouldBe true
        c2.intersects(c1) shouldBe true
    }

    @Test
    fun testPolygonContainmentRespectHoles() {
        val outer = circle2polygon(50, 52.0, 13.0, 10.0)[0]
        val inner = circle2polygon(50, 52.0, 13.0, 5.0)[0]
        val poly = arrayOf(outer, inner).asGeometry
        assertSoftly {
            poly.randomPoints().take(2000).forEach {
                withClue("outer should contain ${it.latitude} ${it.longitude}") {
                    polygonContains(it, outer) shouldBe true
                }
                withClue("inner should not contain ${it.latitude} ${it.longitude}") {
                    polygonContains(it, inner) shouldBe false
                }
                withClue("geometry should contain ${it.latitude} ${it.longitude}") {
                    poly.contains(it) shouldBe true
                }
                withClue("inner geometry should not contain ${it.latitude} ${it.longitude}") {
                    arrayOf(inner).polygonGeometry().contains(it) shouldBe false
                }
            }
        }
    }
}
