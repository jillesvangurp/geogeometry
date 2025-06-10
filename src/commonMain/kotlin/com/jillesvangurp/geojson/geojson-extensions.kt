package com.jillesvangurp.geojson

import com.jillesvangurp.geogeometry.geometry.*

fun LineStringCoordinates.centroid(): DoubleArray {
    var minLon = 180.0
    var maxLon = -180.0
    var minLat = 90.0
    var maxLat = -90.0
    for (coordinate in this) {
        val lon = coordinate.longitude
        if(lon<minLon) minLon = lon
        if(lon>maxLon) maxLon = lon
        val lat = coordinate.latitude
        if(lat<minLat) minLat = lat
        if(lat>maxLat) maxLat = lat

    }
    return doubleArrayOf((minLon+maxLon)/2.0, (minLat+maxLat)/2.0)
}

fun PolygonCoordinates.centroid() = this[0].centroid()
fun MultiPolygonCoordinates.centroid() = this.map { it.centroid() }.toTypedArray().centroid()

fun Geometry.centroid(): PointCoordinates = centroidOrNull()

fun Geometry.centroidOrNull(): PointCoordinates = when (this) {
    is Geometry.Point -> this.coordinates ?: polygonCenter(bbox!!)
    is Geometry.LineString -> this.coordinates?.centroid() ?: polygonCenter(bbox!!)
    is Geometry.MultiLineString -> this.coordinates?.centroid() ?: polygonCenter(bbox!!)
    is Geometry.Polygon -> this.coordinates?.centroid() ?: polygonCenter(bbox!!)
    is Geometry.MultiPolygon -> this.coordinates?.centroid() ?: polygonCenter(bbox!!)
    is Geometry.MultiPoint -> this.coordinates?.centroid() ?: polygonCenter(bbox!!)
    is Geometry.GeometryCollection -> {
        this.geometries.map { it.centroidOrNull() }.toTypedArray().centroid()
    }
}

fun PointCoordinates.translate(latDistance: Double, lonDistance: Double): PointCoordinates = translate(this.latitude, this.longitude, latDistance, lonDistance)
fun Array<PointCoordinates>.translate(latDistance: Double, lonDistance: Double): Array<PointCoordinates> = this.map { it.translate(latDistance,lonDistance) }.toTypedArray()
fun PolygonCoordinates.translate(latDistance: Double, lonDistance: Double): PolygonCoordinates = this.map { it.translate(latDistance,lonDistance) }.toTypedArray()
fun MultiPolygonCoordinates.translate(latDistance: Double, lonDistance: Double): MultiPolygonCoordinates = this.map { it.translate(latDistance,lonDistance) }.toTypedArray()

fun Geometry.translate(newCentroid: Geometry.Point): Geometry {
    val oldCentroid = centroid()
    val compassDirection = headingFromTwoPoints(oldCentroid, newCentroid.coordinates!!)
    // make sure we handle negative distances correctly
    val latFactor = if(compassDirection>90 && compassDirection<270) -1.0 else 1.0
    val lonFactor = if(compassDirection>180) -1.0 else 1.0
    val latDistance = latFactor * distance(oldCentroid, doubleArrayOf(oldCentroid.longitude, newCentroid.coordinates.latitude))
    val lonDistance = lonFactor * distance(oldCentroid, doubleArrayOf(newCentroid.coordinates.longitude, oldCentroid.latitude))
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
    is Geometry.Polygon -> this.copy(coordinates = coordinates!!.translate(latDistance, lonDistance))
    is Geometry.MultiPolygon -> {
        val toTypedArray = coordinates!!.map { it }.toTypedArray()
        val translated = toTypedArray.translate(latDistance, lonDistance)
        this.copy(coordinates = translated)
    }
    is Geometry.GeometryCollection -> {
        this.copy(geometries = this.geometries.map {
            it.translate(latDistance, lonDistance)
        }.toTypedArray())
    }
}

fun PointCoordinates.distanceTo(pointCoordinates: PointCoordinates) = distance(this,pointCoordinates)
fun PointCoordinates.headingTo(pointCoordinates: PointCoordinates) = headingFromTwoPoints(this,pointCoordinates)

fun Geometry.area() = when(this) {
    is Geometry.Polygon -> {
        area(this.coordinates!!)
    }
    is Geometry.MultiPolygon -> this.coordinates?.sumOf {
        area(it)
    } ?:0.0
    else -> 0.0
}

val Geometry.asFeatureCollection get() = FeatureCollection(listOf(this.asFeature()))

fun <T: Geometry> T.scaleX(percent: Double): T {
    @Suppress("UNCHECKED_CAST") // it's fine, generics confusing things
    return when(this) {
        is Geometry.Point -> this
        is Geometry.LineString -> this.copy(coordinates = coordinates?.scaleX(percent)) as T
        is Geometry.MultiLineString -> this.copy(coordinates = coordinates?.scaleX(percent)) as T
        is Geometry.MultiPoint -> this.copy(coordinates = coordinates?.scaleX(percent)) as T
        is Geometry.Polygon -> this.copy(coordinates = coordinates?.scaleX(percent)) as T
        is Geometry.MultiPolygon -> this.copy(coordinates = coordinates?.scaleX(percent)) as T
        is Geometry.GeometryCollection -> this.copy(geometries = geometries.map { it.scaleX(percent) }.toTypedArray()) as T
        else -> error("shouldn't happen")
    }
}

fun  Array<PointCoordinates>.scaleX(percent: Double): Array<PointCoordinates> {
    val centroid = centroid()
    return map { p ->
        val distanceX = p.distanceTo(doubleArrayOf(centroid.longitude, p.latitude))
        val newDistance = distanceX * (percent/(100.0))
        val translateX = distanceX - newDistance
        val left = p.longitude <= centroid.longitude

        val direction = if(left) 1 else -1

        p.translate(0.0, translateX *direction)
    }.toTypedArray()
}

fun  Array<Array<PointCoordinates>>.scaleX(percent: Double): Array<Array<PointCoordinates>> {
    return map { ps ->
        ps.scaleX(percent)
    }.toTypedArray()
}

fun  Array<Array<Array<PointCoordinates>>>.scaleX(percent: Double): Array<Array<Array<PointCoordinates>>> {
    return map { ps ->
        ps.scaleX(percent)
    }.toTypedArray()
}

fun  Array<Array<Array<Array<PointCoordinates>>>>.scaleX(percent: Double): Array<Array<Array<Array<PointCoordinates>>>> {
    return map { ps ->
        ps.scaleX(percent)
    }.toTypedArray()
}

fun Feature.scaleX(percent: Double) = copy(geometry=geometry?.scaleX(percent))
fun FeatureCollection.scaleX(percent: Double) = copy(features = features.map { it.scaleX(percent) })


fun <T: Geometry> T.scaleY(percent: Double): T {
    @Suppress("UNCHECKED_CAST") // it's fine, generics confusing things
    return when(this) {
        is Geometry.Point -> this
        is Geometry.LineString -> this.copy(coordinates = coordinates?.scaleY(percent)) as T
        is Geometry.MultiLineString -> this.copy(coordinates = coordinates?.scaleY(percent)) as T
        is Geometry.MultiPoint -> this.copy(coordinates = coordinates?.scaleY(percent)) as T
        is Geometry.Polygon -> this.copy(coordinates = coordinates?.scaleY(percent)) as T
        is Geometry.MultiPolygon -> this.copy(coordinates = coordinates?.scaleY(percent)) as T
        is Geometry.GeometryCollection -> this.copy(geometries = geometries.map { it.scaleY(percent) }.toTypedArray()) as T
        else -> error("shouldn't happen")
    }
}

fun  Array<PointCoordinates>.scaleY(percent: Double): Array<PointCoordinates> {
    val centroid = centroid()
    return map { p ->
        val distanceY = p.distanceTo(doubleArrayOf(p.longitude, centroid.latitude))
        val newDistance = distanceY * (percent/(100.0))
        val translateY = distanceY - newDistance
        val above = p.latitude >= centroid.latitude
        val direction = if(above) -1 else 1

        p.translate(translateY *direction,0.0)
    }.toTypedArray()
}

fun  Array<Array<PointCoordinates>>.scaleY(percent: Double): Array<Array<PointCoordinates>> {
    return map { ps ->
        ps.scaleY(percent)
    }.toTypedArray()
}

fun  Array<Array<Array<PointCoordinates>>>.scaleY(percent: Double): Array<Array<Array<PointCoordinates>>> {
    return map { ps ->
        ps.scaleY(percent)
    }.toTypedArray()
}

fun  Array<Array<Array<Array<PointCoordinates>>>>.scaleY(percent: Double): Array<Array<Array<Array<PointCoordinates>>>> {
    return map { ps ->
        ps.scaleY(percent)
    }.toTypedArray()
}

fun Feature.scaleY(percent: Double) = copy(geometry=geometry?.scaleY(percent))
fun FeatureCollection.scaleY(percent: Double) = copy(features = features.map { it.scaleY(percent) })

fun <T: Geometry> T.rotate(degrees: Double, around: PointCoordinates?=null): T {
    @Suppress("UNCHECKED_CAST") // it's fine, generics confusing things
    return when(this) {
        is Geometry.Point -> this.copy(coordinates = coordinates?.rotate(degrees,around)) as T
        is Geometry.LineString -> this.copy(coordinates = coordinates?.rotate(degrees,around)) as T
        is Geometry.MultiLineString -> this.copy(coordinates = coordinates?.rotate(degrees,around)) as T
        is Geometry.MultiPoint -> this.copy(coordinates = coordinates?.rotate(degrees,around)) as T
        is Geometry.Polygon -> this.copy(coordinates = coordinates?.rotate(degrees,around)) as T
        is Geometry.MultiPolygon -> this.copy(coordinates = coordinates?.rotate(degrees,around)) as T
        is Geometry.GeometryCollection -> this.copy(geometries = geometries.map { it.rotate(degrees,around) }.toTypedArray()) as T
        else -> error("shouldn't happen")
    }
}

fun PointCoordinates.rotate (degrees: Double, around: PointCoordinates?=null): PointCoordinates {
    return if(around == null) {
        this
    } else {
        rotateAround(around, this, degrees)
    }
}

fun Array<PointCoordinates>.rotate(degrees: Double, around: PointCoordinates?=null): Array<PointCoordinates> {
    val centroid = around?:centroid()
    return map { p ->
        p.rotate(degrees, centroid)
    }.toTypedArray()
}

fun  Array<Array<PointCoordinates>>.rotate(degrees: Double, around: PointCoordinates?=null): Array<Array<PointCoordinates>> {
    return map { ps ->
        ps.rotate(degrees,around)
    }.toTypedArray()
}

fun  Array<Array<Array<PointCoordinates>>>.rotate(degrees: Double, around: PointCoordinates?=null): Array<Array<Array<PointCoordinates>>> {
    return map { ps ->
        ps.rotate(degrees, around)
    }.toTypedArray()
}

fun  Array<Array<Array<Array<PointCoordinates>>>>.rotate(degrees: Double, around: PointCoordinates?=null): Array<Array<Array<Array<PointCoordinates>>>> {
    return map { ps ->
        ps.rotate(degrees,around)
    }.toTypedArray()
}

fun Feature.rotate(degrees: Double, around: PointCoordinates?=null) = copy(geometry=geometry?.rotate(degrees,around))
fun FeatureCollection.rotate(degrees: Double, around: PointCoordinates?=null) = copy(features = features.map { it.rotate(degrees,around) })