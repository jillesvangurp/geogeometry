@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.jillesvangurp.geo

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

// FIXME I messed up the order and did something different than geojson
// should be: [sw.lon,sw.lat,ne.lon,ne.lat] (all axes of the most southwesterly point
//    followed by all axes of the more northeasterly point)

val BoundingBox.southLatitude: Double
    get() {
        return this[1]
        // return this[0] // should be 1
    }

val BoundingBox.northLatitude: Double
    get() {
        return this[3]
        // return this[1] // should be 3
    }

val BoundingBox.westLongitude: Double
    get() {
        return this[0]
        // return this[2] // should be 0
    }

val BoundingBox.eastLongitude: Double
    get() {
        return this[2]
        // return this[3] // should be 2
    }

interface Geometry {
    val type: GeometryType
}

data class Feature(val geometry: Geometry?, val properties: Map<String, Any?>? = null, val bbox: BoundingBox? = null) {
    val type = "Feature"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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

data class FeatureCollection(val features: List<Feature>, val bbox: BoundingBox? = null) {
    val type: String = "FeatureCollection"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeatureCollection

        if (features != other.features) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = features.hashCode()
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

enum class GeometryType {
    Point, MultiPoint, LineString, MultiLineString, Polygon, MultiPolygon, GeometryCollection
}

data class Point(val coordinates: PointCoordinates?, val bbox: BoundingBox? = null) : Geometry {
    override val type = GeometryType.Point
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (coordinates != null) {
            if (other.coordinates == null) return false
            if (!coordinates.contentEquals(other.coordinates)) return false
        } else if (other.coordinates != null) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinates?.contentHashCode() ?: 0
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

data class MultiPoint(val coordinates: MultiPointCoordinates?, val bbox: BoundingBox? = null) : Geometry {
    override val type = GeometryType.MultiPoint
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiPoint

        if (coordinates != null) {
            if (other.coordinates == null) return false
            if (!coordinates.contentDeepEquals(other.coordinates)) return false
        } else if (other.coordinates != null) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinates?.contentDeepHashCode() ?: 0
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

data class LineString(val coordinates: LineStringCoordinates? = null, val bbox: BoundingBox? = null) : Geometry {
    override val type = GeometryType.LineString
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LineString

        if (coordinates != null) {
            if (other.coordinates == null) return false
            if (!coordinates.contentDeepEquals(other.coordinates)) return false
        } else if (other.coordinates != null) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinates?.contentDeepHashCode() ?: 0
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

data class MultiLineString(val coordinates: MultiLineStringCoordinates? = null, val bbox: BoundingBox? = null) :
    Geometry {
    override val type = GeometryType.MultiLineString
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiLineString

        if (coordinates != null) {
            if (other.coordinates == null) return false
            if (!coordinates.contentDeepEquals(other.coordinates)) return false
        } else if (other.coordinates != null) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinates?.contentDeepHashCode() ?: 0
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

data class Polygon(val coordinates: PolygonCoordinates? = null, val bbox: BoundingBox? = null) : Geometry {
    override val type = GeometryType.Polygon
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Polygon

        if (coordinates != null) {
            if (other.coordinates == null) return false
            if (!coordinates.contentDeepEquals(other.coordinates)) return false
        } else if (other.coordinates != null) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinates?.contentDeepHashCode() ?: 0
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

data class MultiPolygon(val coordinates: MultiPolygonCoordinates? = null, val bbox: BoundingBox? = null) : Geometry {
    override val type = GeometryType.MultiPolygon
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultiPolygon

        if (coordinates != null) {
            if (other.coordinates == null) return false
            if (!coordinates.contentDeepEquals(other.coordinates)) return false
        } else if (other.coordinates != null) return false
        if (bbox != null) {
            if (other.bbox == null) return false
            if (!bbox.contentEquals(other.bbox)) return false
        } else if (other.bbox != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = coordinates?.contentDeepHashCode() ?: 0
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

data class GeometryCollection(val geometries: Array<Geometry>, val bbox: BoundingBox? = null) : Geometry {
    override val type = GeometryType.GeometryCollection
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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
