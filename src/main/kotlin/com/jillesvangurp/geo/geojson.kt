package com.jillesvangurp.geo

/**
 * Simple type aliases to have a bit more readable code. Based on https://tools.ietf.org/html/rfc7946#section-3.1.2
 */
typealias Point = DoubleArray
typealias MultiPoint = Array<DoubleArray>
typealias LineString = Array<DoubleArray>
typealias LinearRing = Array<DoubleArray>
typealias MultiLineString = Array<LineString> // Outer polygon + holes
typealias Polygon = Array<LinearRing> // Outer polygon + holes
typealias MultiPolygon = Array<Polygon>

typealias BoundingBox = DoubleArray

val Point.latitude: Double
    get() { return this[1] }

val Point.longitude: Double
    get() { return this[0] }

// FIXME I messed up the order and did something different than geojson
val BoundingBox.southLatitude: Double
    get() { return this[0] }

val BoundingBox.northLatitude: Double
    get() { return this[1] }

val BoundingBox.westLongitude: Double
    get() { return this[2] }

val BoundingBox.eastLongitude: Double
    get() { return this[3] }
