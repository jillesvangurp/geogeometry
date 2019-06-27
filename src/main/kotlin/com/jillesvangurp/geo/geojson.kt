package com.jillesvangurp.geo

typealias Point = DoubleArray
typealias Line = Array<DoubleArray>
typealias Polygon = Array<Array<Line>> // Outer polygon + holes
typealias MultiPolygon = Array<Polygon>

val Point.lat: Double
    get() { return this[1] }

val Point.lon: Double
    get() { return this[0] }
