@file:Suppress("MemberVisibilityCanBePrivate", "unused", "UNCHECKED_CAST")

package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.GeoGeometry.Companion.ensureFollowsRightHandSideRule
import com.jillesvangurp.geo.GeoGeometry.Companion.roundToDecimals
import com.jillesvangurp.geo.GeoHashUtils
import com.jillesvangurp.geojson.Geometry.Polygon
import kotlinx.serialization.KSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.math.*

/**
 * Simple type aliases to have a bit more readable code. Based on https://tools.ietf.org/html/rfc7946#section-3.1.2
 */
typealias PointCoordinates = DoubleArray
/**
 * Lowest axes followed by highest axes
 * BoundingBox = [westLongitude,southLatitude,eastLongitude,northLatitude]
 */
typealias BoundingBox = DoubleArray
typealias MultiPointCoordinates = Array<PointCoordinates>
typealias LineStringCoordinates = Array<PointCoordinates>
typealias LinearRingCoordinates = Array<PointCoordinates>
typealias MultiLineStringCoordinates = Array<LineStringCoordinates> // Outer polygon + holes
typealias PolygonCoordinates = Array<LinearRingCoordinates> // Outer polygon + holes
typealias MultiPolygonCoordinates = Array<PolygonCoordinates>


fun PointCoordinates.isValid(): Boolean {
    return (latitude in -90.0..90.0) && (longitude in -180.0..180.0)
}

fun BoundingBox.isValidBbox(): Boolean {
    // Validate longitude and latitude ranges
    if (westLongitude !in -180.0..180.0 || eastLongitude !in -180.0..180.0) return false
    if (southLatitude !in -90.0..90.0 || northLatitude !in -90.0..90.0) return false

    // Allow bboxes across antimeridian
    return westLongitude != eastLongitude && southLatitude <= northLatitude
}

fun latLon(latitude: Double, longitude: Double): PointCoordinates {
    return doubleArrayOf(longitude, latitude).also {
        require(it.isValid())
    }
}

fun lonLat(longitude: Double, latitude: Double): PointCoordinates = latLon(latitude, longitude)

fun bbox(
    westLongitude: Double,
    southLatitude: Double,
    eastLongitude: Double,
    northLatitude: Double
): BoundingBox {
    val bbox = doubleArrayOf(westLongitude, southLatitude, eastLongitude, northLatitude)
    require(bbox.isValidBbox()) { "Invalid bounding box coordinates" }
    return bbox
}

fun boundingBoxFromTopLeftBottomRight(
    topLeft: PointCoordinates,
    bottomRight: PointCoordinates
): BoundingBox = bbox(
    westLongitude = topLeft.longitude,
    southLatitude = bottomRight.latitude,
    eastLongitude = bottomRight.longitude,
    northLatitude = topLeft.latitude
)

fun boundingBoxFromBottomLeftTopRight(
    bottomLeft: PointCoordinates,
    topRight: PointCoordinates
): BoundingBox = bbox(
    westLongitude = bottomLeft.longitude,
    southLatitude = bottomLeft.latitude,
    eastLongitude = topRight.longitude,
    northLatitude = topRight.latitude
)

fun lineString(vararg pointCoordinates: PointCoordinates): LineStringCoordinates {
    require(pointCoordinates.all { it.size >= 2 }) { "Each point must have at least [lon, lat]" }
    return pointCoordinates as LineStringCoordinates
}

fun validateLinearRing(ring: LinearRingCoordinates) {
    require(ring.size >= 4) { "LinearRing must have at least 4 coordinates (first == last)" }
    require(ring.first().contentEquals(ring.last())) { "LinearRing must be closed (first == last)" }
}

fun linearRingCoordinates(vararg pointCoordinates: PointCoordinates): LinearRingCoordinates {
    val ring = lineString(*if (pointCoordinates.first().contentEquals(pointCoordinates.last()))
        pointCoordinates else (pointCoordinates.toList() + pointCoordinates.first()).toTypedArray())
    validateLinearRing(ring)
    return ring
}

fun multiPoint(vararg points: PointCoordinates): MultiPointCoordinates {
    require(points.all { it.size >= 2 }) { "Each point must have at least [lon, lat]" }
    return points as MultiPointCoordinates
}

fun multiLineString(vararg lineStrings: LineStringCoordinates): MultiLineStringCoordinates {
    lineStrings.forEach { validateLinearRing(it) }
    return lineStrings as MultiLineStringCoordinates
}

fun polygon(vararg outer: PointCoordinates, holes: Array<out LinearRingCoordinates> = emptyArray()): PolygonCoordinates {
    val outerRing = linearRingCoordinates(*outer)
    holes.forEach(::validateLinearRing)
    return arrayOf(outerRing, *holes)
}

fun multiPolygon(vararg polygons: PolygonCoordinates): MultiPolygonCoordinates {
    require(polygons.all { it.isNotEmpty() }) { "Each polygon must have at least one LinearRing" }
    return polygons as MultiPolygonCoordinates
}


fun BoundingBox.toGeometry(): Polygon {
    val coordinates = arrayOf(
        arrayOf(
            doubleArrayOf(this.westLongitude, this.southLatitude),
            doubleArrayOf(this.eastLongitude, this.southLatitude),
            doubleArrayOf(this.eastLongitude, this.northLatitude),
            doubleArrayOf(this.westLongitude, this.northLatitude),
            doubleArrayOf(this.westLongitude, this.southLatitude) // Close the polygon
        )
    )
    return Polygon(coordinates)
}

fun BoundingBox.contains(point: PointCoordinates): Boolean {
    val (longitude, latitude) = point

    // Handle crossing the antimeridian (date line)
    val crossesAntimeridian = this.westLongitude > this.eastLongitude

    val withinLongitude = if (crossesAntimeridian) {
        longitude >= this.westLongitude || longitude <= this.eastLongitude
    } else {
        longitude in this.westLongitude..this.eastLongitude
    }

    val withinLatitude = latitude in this.southLatitude..this.northLatitude

    return withinLongitude && withinLatitude
}

val PolygonCoordinates.asGeometry get() = Polygon(this)

fun PolygonCoordinates.contains(point: PointCoordinates): Boolean =
    GeoGeometry.polygonContains(point.latitude, point.longitude, polygonCoordinatesPoints = this)

fun Geometry.contains(point: PointCoordinates): Boolean {
    return when (this) {
        is Geometry.Point -> this.coordinates?.let {
            it.longitude == point.longitude && it.latitude == point.latitude
        } ?: false

        is Geometry.MultiPoint -> this.coordinates?.any {
            it.longitude == point.longitude && it.latitude == point.latitude
        } ?: false

        is Geometry.LineString -> this.coordinates?.let { coords ->
            (0 until coords.size - 1).any { i ->
                point.onLineSegment(coords[i], coords[i + 1])
            }
        } ?: false

        is Geometry.MultiLineString -> this.coordinates?.any { lineString ->
            (0 until lineString.size - 1).any { i ->
                point.onLineSegment(lineString[i], lineString[i + 1])
            }
        } ?: false

        is Polygon -> this.coordinates?.let { coords ->
            val outer = coords.firstOrNull() ?: return@let false
            val holes = coords.drop(1)
            GeoGeometry.polygonContains(point.latitude, point.longitude, arrayOf(outer)) &&
                    holes.none { hole -> GeoGeometry.polygonContains(point.latitude, point.longitude, arrayOf(hole)) }
        } ?: false

        is Geometry.MultiPolygon -> this.coordinates?.any { polygon ->
            GeoGeometry.polygonContains(point.latitude, point.longitude, polygonCoordinatesPoints = polygon)
        } ?: false

        is Geometry.GeometryCollection -> this.geometries.any { it.contains(point) }
    }
}

fun PointCoordinates.onLineSegment(start: PointCoordinates, end: PointCoordinates): Boolean {
    val crossProduct = (latitude - start.latitude) * (end.longitude - start.longitude) -
            (longitude - start.longitude) * (end.latitude - start.latitude)
    if (abs(crossProduct) > 1e-10) return false

    val dotProduct = (longitude - start.longitude) * (end.longitude - start.longitude) +
            (latitude - start.latitude) * (end.latitude - start.latitude)
    if (dotProduct < 0) return false

    val squaredLength = (end.longitude - start.longitude).pow(2) + (end.latitude - start.latitude).pow(2)
    return dotProduct <= squaredLength
}

fun PointCoordinates.stringify() = "[${this.longitude},${this.latitude}]"
fun LineStringCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"
fun PolygonCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"
fun MultiPolygonCoordinates.stringify() = "[${this.joinToString(", ") { it.stringify() }}]"

fun PointCoordinates.geometry() = Geometry.Point(coordinates = this)
fun MultiPointCoordinates.multiPointGeometry() = Geometry.MultiPoint(coordinates = this)
fun LineStringCoordinates.lineStringGeometry() = Geometry.LineString(coordinates = this)
fun MultiLineStringCoordinates.multiLineStringGeometry() = Geometry.MultiLineString(coordinates = this)
fun PolygonCoordinates.polygonGeometry() = Polygon(coordinates = this)
fun MultiPolygonCoordinates.geometry() = Geometry.MultiPolygon(coordinates = this)

val LinearRingCoordinates.segments
    get() =
        this.indices.map { index ->
            arrayOf(this[index], this[(index + 1) % this.size])
        }

val PolygonCoordinates.outerSegments get() = this[0].segments

fun Geometry.ensureFollowsRightHandSideRule() = when (this) {
    is Polygon -> this.copy(coordinates = this.coordinates?.ensureFollowsRightHandSideRule())
    is Geometry.MultiPolygon -> this.copy(coordinates = this.coordinates?.ensureFollowsRightHandSideRule())
    else -> this
}

fun Geometry.ensureHasAltitude(): Geometry = when (this) {
    is Geometry.Point -> {
        if (this.coordinates?.size == 3) this
        else this.copy(coordinates = this.coordinates?.let { it + doubleArrayOf(0.0) })
    }

    is Geometry.MultiPoint -> {
        this.copy(coordinates = this.coordinates?.map {
            if (it.size == 3) it else it + doubleArrayOf(0.0)
        }?.toTypedArray())
    }

    is Geometry.LineString -> {
        this.copy(coordinates = this.coordinates?.map {
            if (it.size == 3) it else it + doubleArrayOf(0.0)
        }?.toTypedArray())
    }

    is Geometry.MultiLineString -> {
        this.copy(coordinates = this.coordinates?.map { line ->
            line.map {
                if (it.size == 3) it else it + doubleArrayOf(0.0)
            }.toTypedArray()
        }?.toTypedArray())
    }

    is Polygon -> {
        this.copy(coordinates = this.coordinates?.map { ring ->
            ring.map {
                if (it.size == 3) it else it + doubleArrayOf(0.0)
            }.toTypedArray()
        }?.toTypedArray())
    }

    is Geometry.MultiPolygon -> {
        this.copy(coordinates = this.coordinates?.map { polygon ->
            polygon.map { ring ->
                ring.map {
                    if (it.size == 3) it else it + doubleArrayOf(0.0)
                }.toTypedArray()
            }.toTypedArray()
        }?.toTypedArray())
    }

    is Geometry.GeometryCollection -> {
        this.copy(geometries = this.geometries.map { it.ensureHasAltitude() }.toTypedArray())
    }
}

fun Geometry.bbox(): BoundingBox =
    when (this) {
        is Geometry.GeometryCollection -> {
            val bboxes = geometries.map { it.bbox() }
            if (bboxes.isEmpty()) error("Cannot compute bounding box for an empty GeometryCollection")

            val minLongitude = bboxes.minOf { it.westLongitude }
            val minLatitude = bboxes.minOf { it.southLatitude }
            val maxLongitude = bboxes.maxOf { it.eastLongitude }
            val maxLatitude = bboxes.maxOf { it.northLatitude }

            doubleArrayOf(minLongitude, minLatitude, maxLongitude, maxLatitude)
        }

        is Geometry.LineString -> GeoGeometry.boundingBox(coordinates ?: error("no coordinates"))
        is Geometry.MultiLineString -> GeoGeometry.boundingBox(coordinates ?: error("no coordinates"))
        is Geometry.MultiPoint -> GeoGeometry.boundingBox(coordinates ?: error("no coordinates"))
        is Geometry.MultiPolygon -> GeoGeometry.boundingBox(coordinates ?: error("no coordinates"))
        is Geometry.Point -> GeoGeometry.boundingBox(coordinates ?: error("no coordinates"))
        is Polygon -> GeoGeometry.boundingBox(coordinates ?: error("no coordinates"))
    }

val PointCoordinates.latitude: Double
    get() = this[1]
val PointCoordinates.y get() = latitude

val PointCoordinates.longitude: Double
    get() = this[0]
val PointCoordinates.x get() = longitude

fun PointCoordinates.normalize(): PointCoordinates {
    return if (longitude < -180.0 || longitude > 180.0 || latitude < -90.0 || latitude > 90.0) {
        doubleArrayOf(
            // Longitude normalization
            ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0,
            // Latitude normalization with modulo to account for multiple rotations (edge case)
            when (val lat = ((latitude + 90.0) % 360.0 + 360.0) % 360.0 - 90.0) {
                in 90.0..180.0 -> {
                    180.0 - lat
                }

                in -180.0..-90.0 -> {
                    -180.0 - lat
                }

                else -> {
                    lat
                }
            }
        )
    } else {
        this
    }
}

enum class CompassDirection(val letter: Char) { East('E'), West('W'), South('S'), North('N') }

typealias Degree = Double

val Degree.degree: Int get() = floor(abs(this)).roundToInt()
val Degree.minutes: Int get() = floor(((abs(this) - degree.toDouble())) * 60).roundToInt()
val Degree.seconds: Double get() = (abs(this) - degree - minutes / 60.0) * 60.0 * 60
val Degree.northOrSouth: CompassDirection get() = if (this >= 0) CompassDirection.North else CompassDirection.South
val Degree.eastOrWest: CompassDirection get() = if (this >= 0) CompassDirection.East else CompassDirection.West

fun PointCoordinates.humanReadable(): String {
    return """${latitude.degree}° ${latitude.minutes}' ${
        roundToDecimals(
            latitude.seconds,
            2
        )
    }" ${latitude.northOrSouth.letter}, ${longitude.degree}° ${longitude.minutes}' ${
        roundToDecimals(
            longitude.seconds,
            2
        )
    }" ${longitude.eastOrWest.letter}"""
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

fun BoundingBox.polygon(): Polygon {
    val coordinates = arrayOf(
        arrayOf(
            doubleArrayOf(this.westLongitude, this.southLatitude),
            doubleArrayOf(this.eastLongitude, this.southLatitude),
            doubleArrayOf(this.eastLongitude, this.northLatitude),
            doubleArrayOf(this.westLongitude, this.northLatitude),
            doubleArrayOf(this.westLongitude, this.southLatitude)
        )
    )
    return Polygon(coordinates)
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
    val lngFraction = if (lngDiff < 0) {
        (lngDiff + 360) / 360
    } else {
        (lngDiff / 360)
    }

    val globePixelSize = 256 // Google's world dimensions in pixels at zoom level 0 for the globe
    val latZoom = zoom(height, globePixelSize, latFraction)
    val lngZoom = zoom(width, globePixelSize, lngFraction)

    return minOf(latZoom, lngZoom, minZoom)
}

// extension function to set a few supported properties on feature properties
// note, not everything is supported in geojson.io
// https://github.com/mapbox/simplestyle-spec/tree/master/1.1.0
fun JsonObjectBuilder.markerColor(color: String = "red") = put("marker-color", color)

/**
 * Set size of small, medium, large
 */
fun JsonObjectBuilder.markerSize(size: String) = put("marker-size", size)
fun JsonObjectBuilder.markerSymbol(symbol: String) = put("marker-symbol", symbol)
fun JsonObjectBuilder.symbolColor(color: String) = put("symbol-color", color)
fun JsonObjectBuilder.stroke(color: String) = put("stroke", color)
fun JsonObjectBuilder.strokeOpacity(opacity: Double) = put("stroke-opacity", opacity)
fun JsonObjectBuilder.strokeWidth(width: Double) = put("stroke-opacity", width)
fun JsonObjectBuilder.fill(color: String) = put("fill", color)
fun JsonObjectBuilder.fillOpacity(opacity: Double) = put("fill-opacity", opacity)
fun JsonObjectBuilder.title(title: String) = put("title", title)
fun JsonObjectBuilder.description(description: String) = put("description", description)

fun Geometry.asFeature(
    properties: JsonObject? = null,
    bbox: BoundingBox? = null,
): Feature {
    return Feature(this, properties, bbox)
}

fun Geometry.asFeatureWithProperties(
    bbox: BoundingBox? = null,
    propertiesBuilder: (JsonObjectBuilder.() -> Unit)
): Feature {
    val ps = buildJsonObject {
        propertiesBuilder.invoke(this)
    }

    return Feature(this, ps, bbox)
}

private fun deepEquals(left: Array<*>?, right: Array<*>?): Boolean {
    // for some reason the kotlin compiler freaks out over right == null and  insists there is no equals method
    // so hack around it with right?.let { false } ?: true, which is ugly
    return left?.contentDeepEquals(right) ?: right?.let { false } ?: true
}

private fun deepEquals(left: DoubleArray?, right: DoubleArray?): Boolean {
    // for some reason the kotlin compiler freaks out over right == null and  insists there is no equals method
    // so hack around it with right?.let { false } ?: true, which is ugly
    return left?.contentEquals(right) ?: right?.let { false } ?: true
}

infix fun Geometry.Point.line(other: Geometry.Point) = arrayOf(this.coordinates, other.coordinates)
operator fun Geometry.GeometryCollection.plus(other: Geometry.GeometryCollection) =
    Geometry.GeometryCollection(this.geometries + other.geometries)

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = GeometrySerializer::class)
sealed class Geometry {

    abstract val type: GeometryType

    @Serializable
    @SerialName("Point")
    data class Point(
        val coordinates: PointCoordinates?,
        val bbox: BoundingBox? = null,
        @Required
        override val type: GeometryType = GeometryType.Point,
    ) : Geometry() {
        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as Point
                    deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int {
            var result = coordinates?.contentHashCode() ?: 0
            result = 31 * result + (bbox?.contentHashCode() ?: 0)
            return result
        }

        override fun toString(): String = Json.encodeToString(Geometry.serializer(), this)

        companion object {

            fun featureOf(lon: Double, lat: Double) = of(lon, lat).asFeature()

            fun of(lon: Double, lat: Double) = Point(coordinates = doubleArrayOf(lon, lat))
        }
    }

    @Serializable
    @SerialName("MultiPoint")
    data class MultiPoint(
        val coordinates: MultiPointCoordinates?,
        val bbox: BoundingBox? = null,
        @Required
        override val type: GeometryType = GeometryType.MultiPoint,
    ) : Geometry() {
        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as MultiPoint
                    deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(Geometry.serializer(), this)
    }

    @Serializable
    @SerialName("LineString")
    data class LineString(
        val coordinates: LineStringCoordinates? = null,
        val bbox: BoundingBox? = null,
        @Required
        override val type: GeometryType = GeometryType.LineString,
    ) : Geometry() {
        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as LineString
                    deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(Geometry.serializer(), this)
    }

    @Serializable
    @SerialName("MultiLineString")
    data class MultiLineString(
        val coordinates: MultiLineStringCoordinates? = null,
        val bbox: BoundingBox? = null,
        @Required
        override val type: GeometryType = GeometryType.MultiLineString,
    ) : Geometry() {
        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as MultiLineString
                    deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(Geometry.serializer(), this)
    }

    @Serializable
    @SerialName("Polygon")
    data class Polygon(
        // work around for a bug in kotlinx serialization with multi dimensional arrays
        @SerialName("coordinates")
        val coordinates: PolygonCoordinates? = null,
        val bbox: BoundingBox? = null,
        @Required
        override val type: GeometryType = GeometryType.Polygon,
    ) : Geometry() {

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as Polygon
                    deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(Geometry.serializer(), this)
    }


    @Serializable
    @SerialName("MultiPolygon")
    data class MultiPolygon(
        // work around for a bug in kotlinx serialization with multi dimensional arrays
        val coordinates: MultiPolygonCoordinates? = null,
        val bbox: BoundingBox? = null,
        @Required
        override val type: GeometryType = GeometryType.MultiPolygon,
    ) : Geometry() {

        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other == null || this::class != other::class -> false
                else -> {
                    other as MultiPolygon
                    deepEquals(coordinates, other.coordinates) && deepEquals(bbox, other.bbox)
                }
            }
        }

        override fun hashCode(): Int = coordinates.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(Geometry.serializer(), this)
    }

    @Serializable
    @SerialName("GeometryCollection")
    data class GeometryCollection(
        val geometries: Array<Geometry>,
        val bbox: BoundingBox? = null,
        @Required
        override val type: GeometryType = GeometryType.GeometryCollection,
    ) : Geometry() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as GeometryCollection

            if (!geometries.contentEquals(other.geometries)) return false
            if (bbox != null) {
                if (other.bbox == null) return false
                if (!bbox.contentEquals(other.bbox)) return false
            } else if (other.bbox != null) return false

            return true
        }

        override fun hashCode(): Int = geometries.hashCode() + bbox.hashCode()

        override fun toString(): String = Json.encodeToString(Geometry.serializer(), this)
    }
}

object GeometrySerializer : KSerializer<Geometry> {
    override val descriptor = buildClassSerialDescriptor("Geometry")

    override fun serialize(encoder: Encoder, value: Geometry) {
        if (encoder !is JsonEncoder) {
            encoder.encodeString(Json.encodeToString(this, value))
            return
        }

        val element = when (value) {
            is Geometry.Point -> encoder.json.encodeToJsonElement(Geometry.Point.serializer(), value)
            is Geometry.MultiPoint -> encoder.json.encodeToJsonElement(Geometry.MultiPoint.serializer(), value)
            is Geometry.LineString -> encoder.json.encodeToJsonElement(Geometry.LineString.serializer(), value)
            is Geometry.MultiLineString -> encoder.json.encodeToJsonElement(Geometry.MultiLineString.serializer(), value)
            is Geometry.Polygon -> encoder.json.encodeToJsonElement(Geometry.Polygon.serializer(), value)
            is Geometry.MultiPolygon -> encoder.json.encodeToJsonElement(Geometry.MultiPolygon.serializer(), value)
            is Geometry.GeometryCollection ->
                encoder.json.encodeToJsonElement(Geometry.GeometryCollection.serializer(), value)
        }
        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): Geometry {
        if (decoder !is JsonDecoder) {
            return Json.decodeFromString(this, decoder.decodeString())
        }
        val element = decoder.decodeJsonElement()
        val type = element.jsonObject["type"]?.jsonPrimitive?.content
            ?: throw SerializationException("Missing geometry type")

        return when (type) {
            GeometryType.Point.name -> decoder.json.decodeFromJsonElement(Geometry.Point.serializer(), element)
            GeometryType.MultiPoint.name -> decoder.json.decodeFromJsonElement(Geometry.MultiPoint.serializer(), element)
            GeometryType.LineString.name -> decoder.json.decodeFromJsonElement(Geometry.LineString.serializer(), element)
            GeometryType.MultiLineString.name ->
                decoder.json.decodeFromJsonElement(Geometry.MultiLineString.serializer(), element)

            GeometryType.Polygon.name -> decoder.json.decodeFromJsonElement(Geometry.Polygon.serializer(), element)
            GeometryType.MultiPolygon.name ->
                decoder.json.decodeFromJsonElement(Geometry.MultiPolygon.serializer(), element)

            GeometryType.GeometryCollection.name ->
                decoder.json.decodeFromJsonElement(Geometry.GeometryCollection.serializer(), element)

            else -> throw SerializationException("Unsupported geometry type: $type")
        }
    }
}

val PolygonCoordinates.outerCoordinates get() = this[0]
val PolygonCoordinates.holeCoordinates get() = this.slice(1..<this.size)

val Polygon.outerCoordinates get() = coordinates?.outerCoordinates ?: error("no points found")
val Polygon.holeCoordinates get() = coordinates?.holeCoordinates ?: error("no points found")


@Serializable
data class Feature(
    val geometry: Geometry?,
    @Required
    val properties: JsonObject? = null,
    val bbox: BoundingBox? = null,
    val id: String? = null,
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
        result = 31 * result + "Feature".hashCode()
        return result
    }

    override fun toString(): String = Json.encodeToString(serializer(), this)
}

fun Collection<PointCoordinates>.toFeatureCollection(properties: JsonObject? = null) =
    FeatureCollection(map { it.geometry().asFeature(properties) })

fun Collection<Geometry>.asFeatureCollection(properties: JsonObject? = null) =
    FeatureCollection(map { it.asFeature(properties) })

@Serializable
data class FeatureCollection(
    val features: List<Feature>,
    val bbox: BoundingBox? = null
) {
    @Required
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

fun Geometry.randomPoints(): Sequence<PointCoordinates> = sequence {
    fun randomBetween(min: Double, max: Double) = min + kotlin.random.Random.nextDouble() * (max - min)

    when (val geo = this@randomPoints) {
        is Geometry.Point -> geo.coordinates?.let { yield(it) }

        is Geometry.MultiPoint -> geo.coordinates?.forEach { yield(it) }

        is Geometry.LineString -> {
            val coords = geo.coordinates ?: return@sequence
            for (i in 0 until coords.size - 1) {
                val a = coords[i]
                val b = coords[i + 1]
                yield(a)
                yield(b)
                repeat(5) {
                    val t = kotlin.random.Random.nextDouble()
                    yield(
                        doubleArrayOf(
                            a.longitude + t * (b.longitude - a.longitude),
                            a.latitude + t * (b.latitude - a.latitude)
                        )
                    )
                }
            }
        }

        is Geometry.MultiLineString -> {
            val lines = geo.coordinates ?: return@sequence
            for (line in lines) {
                for (i in 0 until line.size - 1) {
                    val a = line[i]
                    val b = line[i + 1]
                    yield(a)
                    yield(b)
                    repeat(5) {
                        val t = kotlin.random.Random.nextDouble()
                        yield(
                            doubleArrayOf(
                                a.longitude + t * (b.longitude - a.longitude),
                                a.latitude + t * (b.latitude - a.latitude)
                            )
                        )
                    }
                }
            }
        }

        is Geometry.MultiPolygon -> {
            val polygons = geo.coordinates ?: return@sequence
            for (poly in polygons) {
                for (p in Geometry.Polygon(poly).randomPoints()) yield(p)
            }
        }

        is Geometry.GeometryCollection -> {
            for (g in geo.geometries) {
                for (p in g.randomPoints()) yield(p)
            }
        }

        is Polygon -> {
            val rings = geo.coordinates ?: return@sequence

            val outer = rings.firstOrNull() ?: return@sequence
            val holes = rings.drop(1)

            // Yield outer ring points in random order
            yieldAll(outer.toList().shuffled())

            // Interpolated points on edges in random order
            val edgeIndices = (0 until outer.size - 1).shuffled()
            for (i in edgeIndices) {
                val a = outer[i]
                val b = outer[i + 1]
                repeat(3) {
                    val t = kotlin.random.Random.nextDouble()
                    yield(
                        doubleArrayOf(
                            a.longitude + t * (b.longitude - a.longitude),
                            a.latitude + t * (b.latitude - a.latitude)
                        )
                    )
                }
            }

            // Sample random points in bounding box, excluding holes
            val bbox = geo.bbox()
            while (true) {
                val p = doubleArrayOf(
                    randomBetween(bbox.westLongitude, bbox.eastLongitude),
                    randomBetween(bbox.southLatitude, bbox.northLatitude)
                )
                if (geo.contains(p) && holes.none { h -> GeoGeometry.polygonContains(p[1], p[0], arrayOf(h)) }) {
                    yield(p)
                }
            }
        }
    }
}

/**
 * Enum with all the types of geometries in https://tools.ietf.org/html/rfc7946#section-3.1.1
 *
 * Note, the names are camel case in the spec and the enum name matches that.
 */
enum class GeometryType {
    Point,
    MultiPoint,
    LineString,
    MultiLineString,
    Polygon,
    MultiPolygon,
    GeometryCollection;
}
