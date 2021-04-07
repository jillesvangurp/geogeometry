@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoHashUtils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KClass

/**
 * Simple type aliases to have a bit more readable code. Based on https://tools.ietf.org/html/rfc7946#section-3.1.2
 */
typealias PointCoordinates = DoubleArray
typealias MultiPointCoordinates = Array<PointCoordinates>
typealias LineStringCoordinates = Array<PointCoordinates>
typealias LinearRingCoordinates = Array<PointCoordinates>
typealias MultiLineStringCoordinates = Array<LineStringCoordinates> // Outer polygon + holes
typealias PolygonCoordinates = Array<LinearRingCoordinates> // Outer polygon + holes
typealias MultiPolygonCoordinates = Array<PolygonCoordinates>

typealias BoundingBox = DoubleArray

fun PointCoordinates.stringify() = "[${this[0]},${this[1]}]"
fun LineStringCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"
fun PolygonCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"
fun MultiPolygonCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"

fun BoundingBox.isValid(): Boolean {
    return this.westLongitude <= this.eastLongitude && this.southLatitude <= this.northLatitude
}

val PointCoordinates.latitude: Double
    get() {
        return this[1]
    }

val PointCoordinates.longitude: Double
    get() {
        return this[0]
    }

val BoundingBox.southLatitude: Double
    get() {
        return this[1]
    }

val BoundingBox.northLatitude: Double
    get() {
        return this[3]
    }

val BoundingBox.westLongitude: Double
    get() {
        return this[0]
    }

val BoundingBox.eastLongitude: Double
    get() {
        return this[2]
    }

fun BoundingBox.polygon(): Geometry.Polygon {
    val coordinates = arrayOf(
        arrayOf(
            doubleArrayOf(this.westLongitude, this.southLatitude),
            doubleArrayOf(this.eastLongitude, this.southLatitude),
            doubleArrayOf(this.eastLongitude, this.northLatitude),
            doubleArrayOf(this.westLongitude, this.northLatitude),
            doubleArrayOf(this.westLongitude, this.southLatitude)
        )
    )
    return Geometry.Polygon(coordinates)
}

fun Geometry.asFeature(properties: JsonObject? = null, bbox: BoundingBox? = null) =
    Feature(this, properties, bbox)

private fun deepEquals(left: Array<*>?, right: Array<*>?): Boolean {
    return left?.contentDeepEquals(right) ?: (right == null)
}

private fun deepEquals(left: DoubleArray?, right: DoubleArray?): Boolean {
    // compiler weirdness right == null produces a reference not found: == ;-/
    return left?.contentEquals(right) ?: right?.equals(null) ?: false
}

@Serializable(with = Geometry.Companion::class)
sealed class Geometry {
    abstract val type: GeometryType

    @Serializable
    data class Point(val coordinates: PointCoordinates?, val bbox: BoundingBox? = null) : Geometry() {
        @Required
        override val type = GeometryType.Point
        infix fun line(other: Point) = arrayOf(this.coordinates, other.coordinates)

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as Point
                    type == other.type && deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int {
            var result = coordinates?.contentHashCode() ?: 0
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }

        companion object {

            fun featureOf(lon: Double, lat: Double) = of(lon, lat).asFeature()


            fun of(lon: Double, lat: Double) = Point(doubleArrayOf(lon, lat))
        }
    }

    @Serializable
    data class MultiPoint(val coordinates: MultiPointCoordinates?, val bbox: BoundingBox? = null) : Geometry() {
        @Required
        override val type = GeometryType.MultiPoint
        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as MultiPoint
                    type == other.type && deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int {
            var result = coordinates?.contentDeepHashCode() ?: 0
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }
    }


    @Serializable
    data class LineString(val coordinates: LineStringCoordinates? = null, val bbox: BoundingBox? = null) :
        Geometry() {
        @Required
        override val type = GeometryType.LineString

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as LineString
                    type == other.type && deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int {
            var result = coordinates?.contentDeepHashCode() ?: 0
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }
    }

    @Serializable
    data class MultiLineString(
        val coordinates: MultiLineStringCoordinates? = null,
        val bbox: BoundingBox? = null
    ) : Geometry() {
        @Required
        override val type = GeometryType.MultiLineString

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as MultiLineString
                    type == other.type && deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int {
            var result = coordinates?.contentDeepHashCode() ?: 0
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }
    }

    @Serializable
    data class Polygon(val coordinates: PolygonCoordinates? = null, val bbox: BoundingBox? = null) :
        Geometry() {
        @Required
        override val type = GeometryType.Polygon

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as Polygon
                    type == other.type && deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int {
            var result = coordinates?.contentDeepHashCode() ?: 0
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }
    }

    @Serializable
    data class MultiPolygon(val coordinates: MultiPolygonCoordinates? = null, val bbox: BoundingBox? = null) :
        Geometry() {
        @Required
        override val type = GeometryType.MultiPolygon

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as MultiPolygon
                    type == other.type && deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int {
            var result = coordinates?.contentDeepHashCode() ?: 0
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }
    }

    @Serializable
    data class GeometryCollection(val geometries: Array<Geometry>, val bbox: BoundingBox? = null) : Geometry() {
        @Required
        override val type = GeometryType.GeometryCollection

        operator fun plus(other: GeometryCollection) = GeometryCollection(this.geometries + other.geometries)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null) return false
            if (this::class != other::class) return false

            other as GeometryCollection

            if (!geometries.contentEquals(other.geometries)) return false
            if (bbox != null) {
                if (other.bbox == null) return false
                if (!bbox.contentEquals(other.bbox)) return false
            } else if (other.bbox != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = geometries.contentHashCode()
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }
    }

    companion object : KSerializer<Geometry> {
        override fun deserialize(decoder: Decoder): Geometry {
            decoder as JsonDecoder
            val jsonObject = decoder.decodeJsonElement().jsonObject
            return when (decoder.json.decodeFromJsonElement(GeometryType.serializer(), jsonObject["type"]!!)) {
                GeometryType.Point -> decoder.json.decodeFromJsonElement(Point.serializer(), jsonObject)
                GeometryType.MultiPoint -> decoder.json.decodeFromJsonElement(
                    MultiPoint.serializer(),
                    jsonObject
                )
                GeometryType.LineString -> decoder.json.decodeFromJsonElement(
                    LineString.serializer(),
                    jsonObject
                )
                GeometryType.MultiLineString -> decoder.json.decodeFromJsonElement(
                    MultiLineString.serializer(),
                    jsonObject
                )
                GeometryType.Polygon -> decoder.json.decodeFromJsonElement(Polygon.serializer(), jsonObject)
                GeometryType.MultiPolygon -> decoder.json.decodeFromJsonElement(
                    MultiPolygon.serializer(),
                    jsonObject
                )
                GeometryType.GeometryCollection -> decoder.json.decodeFromJsonElement(
                    GeometryCollection.serializer(),
                    jsonObject
                )
            }
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("geometry") {
            element<String>("type")
        }

        override fun serialize(encoder: Encoder, value: Geometry) {
            encoder as JsonEncoder
            val jsonElement = when (value) {
                is Point -> encoder.json.encodeToJsonElement(Point.serializer(), value)
                is MultiPoint -> encoder.json.encodeToJsonElement(MultiPoint.serializer(), value)
                is LineString -> encoder.json.encodeToJsonElement(LineString.serializer(), value)
                is MultiLineString -> encoder.json.encodeToJsonElement(
                    MultiLineString.serializer(),
                    value
                )
                is Polygon -> encoder.json.encodeToJsonElement(Polygon.serializer(), value)
                is MultiPolygon -> encoder.json.encodeToJsonElement(MultiPolygon.serializer(), value)
                is GeometryCollection -> encoder.json.encodeToJsonElement(GeometryCollection.serializer(), value)
            }
            encoder.encodeJsonElement(jsonElement)
        }

    }
}

@Serializable
data class Feature(val geometry: Geometry?, val properties: JsonObject? = null, val bbox: BoundingBox? = null) {
    val type = "Feature"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as Feature

        if (geometry != other.geometry) return false
        if (properties != other.properties) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = geometry?.hashCode() ?: 0
        result = 31 * result + (properties?.hashCode() ?: 0)
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }
}

@Serializable
data class FeatureCollection(val features: List<Feature>, val bbox: BoundingBox? = null) {
    val type: String = "FeatureCollection"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as FeatureCollection

        if (features != other.features) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    operator fun plus(other: FeatureCollection) = FeatureCollection(this.features + other.features)

    override fun hashCode(): Int {
        var result = features.hashCode()
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }

    companion object {

        fun fromGeoHashes(hashes: Iterable<String>) =
            FeatureCollection(hashes.map { GeoHashUtils.decodeBbox(it).polygon() }.toList().map { it.asFeature() })

        fun of(vararg features: Feature) = FeatureCollection(features.toList())
    }
}

/**
 * Enum with all the types of geometries in https://tools.ietf.org/html/rfc7946#section-3.1.1
 *
 * Note, the names are camel case in the spec and the enum name matches that.
 */
@Serializable
enum class GeometryType {

    Point,
    MultiPoint,
    LineString,
    MultiLineString,
    Polygon,
    MultiPolygon,
    GeometryCollection;

    fun clazz(): KClass<*> = when (this) {
        Point -> Geometry.Point::class
        MultiPoint -> Geometry.MultiPoint::class
        LineString -> Geometry.LineString::class
        MultiLineString -> Geometry.MultiLineString::class
        Polygon -> Geometry.Polygon::class
        MultiPolygon -> Geometry.MultiPolygon::class
        GeometryCollection -> Geometry.GeometryCollection::class
    }
}

