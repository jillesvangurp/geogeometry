@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.GeoGeometry.Companion.ensureFollowsRightHandSideRule
import com.jillesvangurp.geo.GeoGeometry.Companion.roundToDecimals
import com.jillesvangurp.geo.GeoHashUtils
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.math.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
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
private typealias PolygonCoordinatesList = List<LinearRingCoordinates>
private typealias MultiPolygonCoordinatesList = List<PolygonCoordinatesList>

/**
 * Lowest axes followed by highest axes
 * BoundingBox = [westLongitude,southLatitude,eastLongitude,westLatitude]
 */
typealias BoundingBox = DoubleArray

fun PointCoordinates.stringify() = "[${this.longitude},${this.latitude}]"
fun LineStringCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"
fun PolygonCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"
fun MultiPolygonCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"

fun PointCoordinates.geometry() = Geometry.Point(coordinates = this)
fun MultiPointCoordinates.multiPointGeometry() = Geometry.MultiPoint(coordinates = this)
fun LineStringCoordinates.lineStringGeometry() = Geometry.LineString(coordinates = this)
fun MultiLineStringCoordinates.multiLineStringGeometry() = Geometry.MultiLineString(coordinates = this)
fun PolygonCoordinates.polygonGeometry() = Geometry.Polygon(coordinates = this)
fun MultiPolygonCoordinates.geometry() = Geometry.MultiPolygon(coordinates = this)

fun Geometry.ensureFollowsRightHandSideRule() = when (this) {
    is Geometry.Polygon -> this.copy(coordinates = this.coordinates?.asArray?.ensureFollowsRightHandSideRule())
    is Geometry.MultiPolygon -> this.copy(arrayCoordinates = this.coordinates?.asArray?.ensureFollowsRightHandSideRule())
    else -> this
}

fun Geometry.ensureHasAltitude() = when (this) {
    is Geometry.Point -> if (this.coordinates?.size == 3) this else this.copy()
    is Geometry.MultiPoint -> TODO()
    is Geometry.LineString -> TODO()
    is Geometry.MultiLineString -> TODO()
    is Geometry.Polygon -> TODO()
    is Geometry.MultiPolygon -> TODO()
    is Geometry.GeometryCollection -> TODO()
}

fun BoundingBox.isValid(): Boolean {
    return this.westLongitude <= this.eastLongitude && this.southLatitude <= this.northLatitude
}

val PointCoordinates.latitude: Double
    get() = this[1]

val PointCoordinates.longitude: Double
    get() = this[0]

enum class CompassDirection(val letter: Char) { East('E'), West('W'), South('S'), North('N') }

typealias Degree = Double

val Degree.degree: Int get() = floor(abs(this)).roundToInt()
val Degree.minutes: Int get() = floor(((abs(this) - degree.toDouble())) * 60).roundToInt()
val Degree.seconds: Double get() = (abs(this) - degree - minutes / 60.0) * 60.0 * 60
val Degree.northOrSouth: CompassDirection get() = if (this >= 0) CompassDirection.North else CompassDirection.South
val Degree.eastOrWest: CompassDirection get() = if (this >= 0) CompassDirection.East else CompassDirection.West

fun PointCoordinates.humanReadable(): String {
    return """${latitude.degree}° ${latitude.minutes}' ${roundToDecimals(latitude.seconds,2)}" ${latitude.northOrSouth.letter}, ${longitude.degree}° ${longitude.minutes}' ${roundToDecimals(longitude.seconds,2)}" ${longitude.eastOrWest.letter}"""
}
val PointCoordinates.altitude: Double?
    get() = if (this.size == 3) this[2] else null

val BoundingBox.southLatitude: Double
    get() = this[1]

val BoundingBox.northLatitude: Double
    get() = this[3]

val BoundingBox.westLongitude: Double
    get() = this[0]

val BoundingBox.eastLongitude: Double
    get() = this[2]

val BoundingBox.topLeft: PointCoordinates get() = doubleArrayOf(westLongitude, northLatitude)
val BoundingBox.bottomLeft: PointCoordinates get() = doubleArrayOf(westLongitude, southLatitude)
val BoundingBox.topRight: PointCoordinates get() = doubleArrayOf(eastLongitude, northLatitude)
val BoundingBox.bottomRight: PointCoordinates get() = doubleArrayOf(eastLongitude, southLatitude)

val BoundingBox.northEast get() = doubleArrayOf(this.eastLongitude, this.northLatitude)
val BoundingBox.northWest get() = doubleArrayOf(this.westLongitude, this.northLatitude)
val BoundingBox.southEast get() = doubleArrayOf(this.eastLongitude, this.southLatitude)
val BoundingBox.southWest get() = doubleArrayOf(this.westLongitude, this.southLatitude)

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

/**
 * Map zoom level appropriate for a bounding box in a viewport with the specified amount of pixels.
 *
 * https://stackoverflow.com/a/6055653/1041442
 */
fun BoundingBox.zoomLevel(height: Int = 512, width: Int = 512, minZoom: Double = 22.0): Double {

    fun zoom(mapPx: Int, worldPx: Int, fraction: Double) = floor(ln(mapPx / worldPx / fraction) / ln(2.0))

    val latFraction = (GeoGeometry.toRadians(northEast.latitude) - GeoGeometry.toRadians(southWest.latitude)) / PI

    val lngDiff = northEast.longitude - southWest.longitude
    val lngFraction = if (lngDiff < 0) { (lngDiff + 360) / 360 } else { (lngDiff / 360) }

    val globePixelSize = 256 // Google's world dimensions in pixels at zoom level 0 for the globe
    val latZoom = zoom(height, globePixelSize, latFraction)
    val lngZoom = zoom(width, globePixelSize, lngFraction)

    return minOf(latZoom, lngZoom, minZoom)
}

fun Geometry.asFeature(properties: JsonObject? = null, bbox: BoundingBox? = null) =
    Feature(this, properties, bbox)

private fun deepEquals(left: Array<*>?, right: Array<*>?): Boolean {
    // for some reason the kotlin compiler freaks out over right == null and  insists there is no equals method
    // so hack around it with right?.let { false } ?: true, which is ugly
    return left?.let { it.contentDeepEquals(right) } ?: right?.let { false } ?: true
}

private fun deepEquals(left: DoubleArray?, right: DoubleArray?): Boolean {
    // for some reason the kotlin compiler freaks out over right == null and  insists there is no equals method
    // so hack around it with right?.let { false } ?: true, which is ugly
    return left?.let {
        it.contentEquals(right)
    } ?: right?.let { false } ?: true
}

infix fun Geometry.Point.line(other: Geometry.Point) = arrayOf(this.coordinates, other.coordinates)
operator fun Geometry.GeometryCollection.plus(other: Geometry.GeometryCollection) =
    Geometry.GeometryCollection(this.geometries + other.geometries)

@Serializable(with = Geometry.Companion::class)
sealed class Geometry {
    abstract val type: GeometryType

    @Serializable
    @SerialName("Point")
    data class Point(
        val coordinates: PointCoordinates?,
        val bbox: BoundingBox? = null,
    ) : Geometry() {
        @Required
        override val type = GeometryType.Point

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

        override fun toString(): String = Json.encodeToString(serializer(), this)

        companion object {

            fun featureOf(lon: Double, lat: Double) = of(lon, lat).asFeature()

            fun of(lon: Double, lat: Double) = Point(doubleArrayOf(lon, lat))
        }
    }

    @Serializable
    @SerialName("MultiPoint")
    data class MultiPoint(
        val coordinates: MultiPointCoordinates?,
        val bbox: BoundingBox? = null,
    ) : Geometry() {
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

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(serializer(), this)
    }

    @Serializable
    @SerialName("LineString")
    data class LineString(
        val coordinates: LineStringCoordinates? = null,
        val bbox: BoundingBox? = null
    ) : Geometry() {
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

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(serializer(), this)
    }

    @Serializable
    @SerialName("MultiLineString")
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

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(serializer(), this)
    }

    @Serializable
    @SerialName("Polygon")
    data class Polygon(
        // work around for a bug in kotlinx serialization with multi dimensional arrays
        @SerialName("coordinates")
        val coordinates: PolygonCoordinatesList? = null,
        val bbox: BoundingBox? = null
    ) : Geometry() {
        @Required
        override val type = GeometryType.Polygon

        constructor(
            coordinates: PolygonCoordinates? = null,
            bbox: BoundingBox? = null
        ) : this(coordinates = coordinates?.toList(), bbox = bbox)

        fun copy(coordinates: PolygonCoordinates?) = copy(coordinates = coordinates?.toList())

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as Polygon
                    type == other.type && deepEquals(coordinates?.asArray, other.coordinates?.asArray) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(serializer(), this)
    }

    @Serializable
    @SerialName("MultiPolygon")
    data class MultiPolygon(
        // work around for a bug in kotlinx serialization with multi dimensional arrays
        val coordinates: MultiPolygonCoordinatesList? = null,
        val bbox: BoundingBox? = null
    ) : Geometry() {
        @Required
        override val type = GeometryType.MultiPolygon

        constructor(
            coordinates: MultiPolygonCoordinates? = null,
            bbox: BoundingBox? = null
        ) : this(coordinates = coordinates?.map { it.toList() }?.toList(), bbox = bbox)

        fun copy(arrayCoordinates: MultiPolygonCoordinates?) = copy(coordinates = arrayCoordinates?.map { it.toList() }?.toList(), bbox = bbox)

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as MultiPolygon
                    type == other.type && deepEquals(coordinates?.asArray, other.coordinates?.asArray) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(serializer(), this)
    }

    @Serializable
    @SerialName("GeometryCollection")
    data class GeometryCollection(
        val geometries: Array<Geometry>,
        val bbox: BoundingBox? = null,
    ) : Geometry() {
        @Required
        override val type = GeometryType.GeometryCollection

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

        override fun hashCode(): Int = geometries.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(serializer(), this)
    }

    @Serializer(forClass = Geometry::class)
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
                GeometryType.Polygon -> decoder.json.decodeFromJsonElement(
                    Polygon.serializer(),
                    jsonObject
                )
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
data class Feature(
    val geometry: Geometry?,
    @Required
    val properties: JsonObject? = null,
    val bbox: BoundingBox? = null
) {
    @Required
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
        result = 31 * result + (bbox?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String = Json.encodeToString(serializer(), this)
}

@Serializable
data class FeatureCollection(val features: List<Feature>, val bbox: BoundingBox? = null) {
    @Required // forces this to be serialized
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
        result = 31 * result + (bbox?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = Json.encodeToString(serializer(), this)

    companion object {

        fun fromGeoHashes(hashes: Iterable<String>) =
            FeatureCollection(hashes.map { GeoHashUtils.decodeBbox(it).polygon() }.toList().map { it.asFeature() })

        fun of(vararg features: Feature) = FeatureCollection(features.toList())
    }
}

// needed to workaround some serialization bugs with kotlinx serialization
val PolygonCoordinatesList.asArray: PolygonCoordinates get() = toTypedArray()
val MultiPolygonCoordinatesList.asArray: MultiPolygonCoordinates get() = map { it.toTypedArray() }.toTypedArray()

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
