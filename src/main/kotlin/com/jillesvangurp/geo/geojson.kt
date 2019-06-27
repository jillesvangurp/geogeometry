package com.jillesvangurp.geo

typealias Point = DoubleArray
typealias Line = Array<DoubleArray>
typealias Polygon = Array<Array<Line>> // Outer polygon + holes
typealias MultiPolygon = Array<Polygon>
typealias BoundingBox = DoubleArray

val Point.latitude: Double
    get() { return this[1] }

val Point.longitude: Double
    get() { return this[0] }

val BoundingBox.southLatitude: Double
    get() { return this[0] }

val BoundingBox.northLatitude: Double
    get() { return this[1] }

val BoundingBox.westLongitude: Double
    get() { return this[2] }

val BoundingBox.eastLongitude: Double
    get() { return this[3] }