package com.jillesvangurp.geogeometry.core

import kotlin.math.PI

// Types previously in geojson.kt

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

// Constants previously defined in GeoGeometry
const val EARTH_RADIUS_METERS = 6371000.0
const val WGS84_RADIUS = 6378137.0
const val EARTH_CIRCUMFERENCE_METERS = EARTH_RADIUS_METERS * PI * 2.0
const val DEGREE_LATITUDE_METERS = EARTH_RADIUS_METERS * PI / 180.0
const val DEGREES_TO_RADIANS = PI / 180.0
const val RADIANS_TO_DEGREES = 1.0 / DEGREES_TO_RADIANS
