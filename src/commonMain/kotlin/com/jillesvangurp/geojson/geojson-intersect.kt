package com.jillesvangurp.geojson

import com.jillesvangurp.geo.GeoGeometry

fun Geometry.intersects(other: Geometry): Boolean {
    val bbox1 = this.bbox()
    val bbox2 = other.bbox()
    if (!bboxesIntersect(bbox1, bbox2)) return false

    return when (this) {
        is Geometry.Point -> other.contains(this.coordinates ?: return false)
        is Geometry.MultiPoint -> this.coordinates?.any { other.contains(it) } == true
        is Geometry.LineString -> intersectsLine(this.coordinates ?: return false, other)
        is Geometry.MultiLineString -> this.coordinates?.any { intersectsLine(it, other) } == true
        is Geometry.Polygon -> intersectsPolygon(this.coordinates ?: return false, other)
        is Geometry.MultiPolygon -> this.coordinates?.any { intersectsPolygon(it, other) } == true
        is Geometry.GeometryCollection -> this.geometries.any { it.intersects(other) }
    }
}

private fun bboxesIntersect(b1: BoundingBox, b2: BoundingBox): Boolean {
    // Pick the shorter east-going arc for each raw interval
    fun normalize(w: Double, e: Double): Pair<Double, Double> {
        // span measured modulo 360
        val span = ( (e - w + 360) % 360 )
        return if (span > 180) e to w else w to e
    }

    fun lonIntersects(w1: Double, e1: Double, w2: Double, e2: Double): Boolean {
        val (nW1, nE1) = normalize(w1, e1)
        val (nW2, nE2) = normalize(w2, e2)
        // now split only if it still wraps
        val arcs1 = if (nW1 <= nE1) listOf(nW1 to nE1)
        else listOf(nW1 to 180.0, -180.0 to nE1)
        val arcs2 = if (nW2 <= nE2) listOf(nW2 to nE2)
        else listOf(nW2 to 180.0, -180.0 to nE2)

        return arcs1.any { (a0, a1) ->
            arcs2.any { (b0, b1) ->
                !(a0 > b1 || a1 < b0)
            }
        }
    }

    val lonOverlap = lonIntersects(
        b1.westLongitude, b1.eastLongitude,
        b2.westLongitude, b2.eastLongitude
    )
    val latOverlap = !(b1.northLatitude < b2.southLatitude ||
            b1.southLatitude > b2.northLatitude)

    return lonOverlap && latOverlap
}

private fun intersectsLine(line: LineStringCoordinates, other: Geometry): Boolean {
    val segments = line.zipWithNextCompat()
    return segments.any { (start, end) ->
        when (other) {
            is Geometry.Point -> other.coordinates?.onLineSegment(start, end) ?: false
            is Geometry.LineString -> other.coordinates?.zipWithNextCompat()?.any { (oStart, oEnd) ->
                GeoGeometry.linesCross(start, end, oStart, oEnd)
            } == true
            is Geometry.MultiLineString -> other.coordinates?.any { oLine ->
                oLine.zipWithNextCompat().any { (oStart, oEnd) ->
                    GeoGeometry.linesCross(start, end, oStart, oEnd)
                }
            } == true
            is Geometry.Polygon -> other.outerCoordinates.zipWithNextCompat().any { (oStart, oEnd) ->
                GeoGeometry.linesCross(start, end, oStart, oEnd)
            } || other.contains(start)
            is Geometry.MultiPolygon -> other.coordinates?.any { poly ->
                poly.first().zipWithNextCompat().any { (oStart, oEnd) ->
                    GeoGeometry.linesCross(start, end, oStart, oEnd)
                } || GeoGeometry.polygonContains(start.latitude, start.longitude, poly)
            } == true
            is Geometry.GeometryCollection -> other.geometries.any { intersectsLine(line, it) }
            is Geometry.MultiPoint -> {
                other.coordinates?.any {
                    it.onLineSegment(start,end)
                } == true
            }
        }
    }
}

private fun intersectsPolygon(polygon: PolygonCoordinates, other: Geometry): Boolean {
    val outer = polygon.first()
    return outer.zipWithNextCompat().any { (start, end) ->
        intersectsLine(arrayOf(start, end), other)
    } || when (other) {
        is Geometry.Point -> other.coordinates?.let { GeoGeometry.polygonContains(it.latitude, it.longitude, polygon) } ?: false
        is Geometry.MultiPoint -> other.coordinates?.any { GeoGeometry.polygonContains(it.latitude, it.longitude, polygon) } == true
        // any member geometry intersects this polygon
        is Geometry.GeometryCollection ->
            other.geometries.any { intersectsPolygon(polygon, it) }

        // a line (or any vertex of it) wholly inside the polygon
        is Geometry.LineString ->
            other.coordinates?.any {
                GeoGeometry.polygonContains(it.latitude, it.longitude, polygon)
            } == true

        // the same, but for many lines
        is Geometry.MultiLineString ->
            other.coordinates?.any { line ->
                line.any { GeoGeometry.polygonContains(it.latitude, it.longitude, polygon) }
            } == true

        // polygon-vs-polygon – either contains a vertex of the other
        is Geometry.Polygon -> {
            val oCoords = other.coordinates ?: return false
            GeoGeometry.polygonContains(other.outerCoordinates.first().latitude,
                other.outerCoordinates.first().longitude,
                polygon) ||
                    GeoGeometry.polygonContains(outer.first().latitude,
                        outer.first().longitude,
                        oCoords)
        }

        // any constituent polygon intersects or is contained
        is Geometry.MultiPolygon ->
            other.coordinates?.any { oPoly ->
                GeoGeometry.polygonContains(oPoly.first().first().latitude,
                    oPoly.first().first().longitude,
                    polygon) ||
                        GeoGeometry.polygonContains(outer.first().latitude,
                            outer.first().longitude,
                            oPoly)
            } == true
    }
}

private fun <T> Array<T>.zipWithNextCompat(): List<Pair<T, T>> {
    // multiplatform doesn't have zipWithNext
    val result = mutableListOf<Pair<T, T>>()
    for (i in 0 until this.size - 1) {
        result.add(this[i] to this[i + 1])
    }
    return result
}