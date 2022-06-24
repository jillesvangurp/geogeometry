package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoGeometry

fun LineStringCoordinates.centroid(): DoubleArray {
    val lon = this.sumOf { it.longitude } / size
    val lat = this.sumOf { it.latitude } / size
    return doubleArrayOf(lon, lat)
}

fun PolygonCoordinates.centroid() = this[0].centroid()
fun MultiPolygonCoordinates.centroid() = this.map { it.centroid() }.toTypedArray().centroid()

fun Geometry.centroid(): PointCoordinates = centroidOrNull()

fun Geometry.centroidOrNull(): PointCoordinates = when (this) {
    is Geometry.Point -> this.coordinates ?: GeoGeometry.polygonCenter(bbox!!)
    is Geometry.LineString -> this.coordinates?.centroid() ?: GeoGeometry.polygonCenter(bbox!!)
    is Geometry.MultiLineString -> this.coordinates?.centroid() ?: GeoGeometry.polygonCenter(bbox!!)
    is Geometry.Polygon -> this.coordinates?.asArray?.centroid() ?: GeoGeometry.polygonCenter(bbox!!)
    is Geometry.MultiPolygon -> this.coordinates?.asArray?.centroid() ?: GeoGeometry.polygonCenter(bbox!!)
    is Geometry.MultiPoint -> this.coordinates?.centroid() ?: GeoGeometry.polygonCenter(bbox!!)
    is Geometry.GeometryCollection -> {
        this.geometries.map { it.centroidOrNull() }.toTypedArray().centroid()
    }
}

fun PointCoordinates.translate(latDistance: Double, lonDistance: Double): PointCoordinates = GeoGeometry.translate(this.latitude, this.longitude, latDistance, lonDistance)
fun Array<PointCoordinates>.translate(latDistance: Double, lonDistance: Double): Array<PointCoordinates> = this.map { it.translate(latDistance,lonDistance) }.toTypedArray()
fun PolygonCoordinates.translate(latDistance: Double, lonDistance: Double): PolygonCoordinates = this.map { it.translate(latDistance,lonDistance) }.toTypedArray()
fun MultiPolygonCoordinates.translate(latDistance: Double, lonDistance: Double): MultiPolygonCoordinates = this.map { it.translate(latDistance,lonDistance) }.toTypedArray()

fun Geometry.translate(newCentroid: Geometry.Point): Geometry {
    val oldCentroid = centroid()
    val compassDirection = GeoGeometry.headingFromTwoPoints(oldCentroid, newCentroid.coordinates!!)
    // make sure we handle negative distances correctly
    val latFactor = if(compassDirection>180) -1.0 else 1.0
    val lonFactor = if(compassDirection>90 && compassDirection<270) -1.0 else 1.0
    val latDistance = latFactor * GeoGeometry.distance(oldCentroid, doubleArrayOf(oldCentroid.longitude, newCentroid.coordinates.latitude))
    val lonDistance = lonFactor * GeoGeometry.distance(oldCentroid, doubleArrayOf(newCentroid.coordinates.longitude, oldCentroid.latitude))
    return translate(latDistance, lonDistance)
}

fun Geometry.translate(
    latDistance: Double,
    lonDistance: Double
): Geometry = when (this) {
    is Geometry.Point -> this.copy(coordinates = coordinates!!.translate(latDistance, lonDistance))

    is Geometry.MultiPoint -> this.copy(coordinates = coordinates!!.translate(latDistance, lonDistance))
    is Geometry.LineString -> this.copy(coordinates = coordinates!!.translate(latDistance, lonDistance))
    is Geometry.MultiLineString -> this.copy(coordinates = coordinates!!.translate(latDistance, lonDistance))
    is Geometry.Polygon -> this.copy(coordinates = coordinates!!.toTypedArray().translate(latDistance, lonDistance))
    is Geometry.MultiPolygon -> {
        val toTypedArray = coordinates!!.map { it.toTypedArray() }.toTypedArray()
        val translated = toTypedArray.translate(latDistance, lonDistance).toList().map { it.toList() }
        this.copy(coordinates = translated)
    }
    is Geometry.GeometryCollection -> {
        this.copy(geometries = this.geometries.map {
            it.translate(latDistance, lonDistance)
        }.toTypedArray())
    }
}