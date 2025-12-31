package com.jillesvangurp.geo

import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Adapted from MIT licensed https://github.com/Merowech/java-concave-hull
 * by @author Udo Schlegel - Udo.3.Schlegel(at)uni-konstanz.de
 *
 * Code has been converted to Kotlin and refactored a little.
 *
 * Note. It currently  has issues with self intersecting. So, I don't recommend using
 * this in its current form.
 */

private data class MetricPoint(val point: PointCoordinates, val x: Double, val y: Double)

private fun project(points: List<PointCoordinates>): List<MetricPoint> {
    val centerLat = points.map { it.latitude }.average()
    val centerLon = points.map { it.longitude }.average()
    val scale = GeoGeometry.WGS84_RADIUS * PI / 180.0
    val cosLat = cos(centerLat * PI / 180.0)
    return points.map { p ->
        val x = (p.longitude - centerLon) * scale * cosLat
        val y = (p.latitude - centerLat) * scale
        MetricPoint(p, x, y)
    }
}

private fun euclideanDistance(a: MetricPoint, b: MetricPoint): Double {
    return sqrt((a.x - b.x).pow(2.0) + (a.y - b.y).pow(2.0))
}

private fun kNearestNeighbors(
    l: List<MetricPoint>,
    q: MetricPoint,
    k: Int
): List<MetricPoint> {
    val nearestList = mutableListOf<Pair<Double, MetricPoint>>()
    for (o in l) {
        nearestList.add(Pair(euclideanDistance(q, o), o))
    }
    nearestList.sortBy { it.first }

    val result = mutableListOf<MetricPoint>()
    for (i in 0 until min(k, nearestList.size)) {
        result.add(nearestList[i].second)
    }
    return result
}

private fun findMinYPoint(l: List<MetricPoint>): MetricPoint =
    l.minByOrNull { it.y } ?: error("list should not be empty")

private fun calculateAngle(o1: MetricPoint, o2: MetricPoint): Double {
    val angle = atan2(o2.y - o1.y, o2.x - o1.x)
    return (angle + 2 * PI) % (2 * PI)
}

private fun angleDifference(a1: Double, a2: Double): Double {
    val diff = (a2 - a1 + 2 * PI) % (2 * PI)
    return if (diff > PI) 2 * PI - diff else diff
}

private fun sortByAngle(
    l: List<MetricPoint>,
    q: MetricPoint,
    a: Double
): List<MetricPoint> {
    // Sort by angle descending
    return l.sortedBy {
        angleDifference(a, calculateAngle(q, it))
    }
}

private fun orientation(a: MetricPoint, b: MetricPoint, c: MetricPoint): Double {
    return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
}

private fun linesCross(a1: MetricPoint, a2: MetricPoint, b1: MetricPoint, b2: MetricPoint): Boolean {
    val d1 = orientation(a1, a2, b1)
    val d2 = orientation(a1, a2, b2)
    val d3 = orientation(b1, b2, a1)
    val d4 = orientation(b1, b2, a2)
    return (d1 == 0.0 && d2 == 0.0 && d3 == 0.0 && d4 == 0.0).not() &&
        (d1.sign() != d2.sign() && d3.sign() != d4.sign())
}

private fun Double.sign(): Int = when {
    this > 0 -> 1
    this < 0 -> -1
    else -> 0
}

private data class HullAttempt(val hull: List<PointCoordinates>, val success: Boolean)

private fun closeHull(hull: List<PointCoordinates>): List<PointCoordinates> {
    if (hull.isEmpty()) return hull
    return if (hull.first() contentEquals hull.last()) hull else hull + hull.first()
}

private fun orientationValue(a: PointCoordinates, b: PointCoordinates, c: PointCoordinates): Double {
    return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
}

private fun segmentsCrossRobust(a1: PointCoordinates, a2: PointCoordinates, b1: PointCoordinates, b2: PointCoordinates): Boolean {
    val eps = 1e-12
    val o1 = orientationValue(a1, a2, b1)
    val o2 = orientationValue(a1, a2, b2)
    val o3 = orientationValue(b1, b2, a1)
    val o4 = orientationValue(b1, b2, a2)

    val s1 = when {
        o1 > eps -> 1
        o1 < -eps -> -1
        else -> 0
    }
    val s2 = when {
        o2 > eps -> 1
        o2 < -eps -> -1
        else -> 0
    }
    val s3 = when {
        o3 > eps -> 1
        o3 < -eps -> -1
        else -> 0
    }
    val s4 = when {
        o4 > eps -> 1
        o4 < -eps -> -1
        else -> 0
    }
    return (s1 != s2) && (s3 != s4)
}

private fun hasSelfIntersections(ring: List<PointCoordinates>): Boolean {
    if (ring.size < 4) return false
    val closed = closeHull(ring)
    for (i in 0 until closed.size - 1) {
        val a1 = closed[i]
        val a2 = closed[i + 1]
        for (j in i + 1 until closed.size - 1) {
            if (kotlin.math.abs(i - j) <= 1) continue
            if (i == 0 && j == closed.size - 2) continue
            val b1 = closed[j]
            val b2 = closed[j + 1]
            if (segmentsCrossRobust(a1, a2, b1, b2)) {
                return true
            }
        }
    }
    return false
}

private fun hasSelfIntersectionsGeo(ring: List<PointCoordinates>): Boolean {
    if (ring.size < 4) return false
    val closed = closeHull(ring)
    for (i in 0 until closed.size - 1) {
        val a1 = closed[i]
        val a2 = closed[i + 1]
        for (j in i + 1 until closed.size - 1) {
            if (kotlin.math.abs(i - j) <= 1) continue
            if (i == 0 && j == closed.size - 2) continue
            val b1 = closed[j]
            val b2 = closed[j + 1]
            if (GeoGeometry.linesCross(a1, a2, b1, b2)) {
                return true
            }
        }
    }
    return false
}

private fun removeSelfIntersections(hull: List<PointCoordinates>, maxIterations: Int = 10): List<PointCoordinates> {
    if (hull.size < 4) return closeHull(hull)
    val points = closeHull(hull).toMutableList()
    var iteration = 0
    while (iteration < maxIterations) {
        var swapped = false
        for (i in 0 until points.size - 1) {
            for (j in i + 1 until points.size - 1) {
                if (kotlin.math.abs(i - j) <= 1) continue
                if (i == 0 && j == points.size - 2) continue
                if (segmentsCrossRobust(points[i], points[i + 1], points[j], points[j + 1])) {
                    points.subList(i + 1, j + 1).reverse()
                    swapped = true
                    break
                }
            }
            if (swapped) break
        }
        if (!swapped) break
        iteration++
    }
    return closeHull(points)
}

private fun removeSelfIntersectionsGeo(hull: List<PointCoordinates>, maxIterations: Int = 10): List<PointCoordinates> {
    if (hull.size < 4) return closeHull(hull)
    val points = closeHull(hull).toMutableList()
    var iteration = 0
    while (iteration < maxIterations) {
        var swapped = false
        for (i in 0 until points.size - 1) {
            for (j in i + 1 until points.size - 1) {
                if (kotlin.math.abs(i - j) <= 1) continue
                if (i == 0 && j == points.size - 2) continue
                if (GeoGeometry.linesCross(points[i], points[i + 1], points[j], points[j + 1])) {
                    points.subList(i + 1, j + 1).reverse()
                    swapped = true
                    break
                }
            }
            if (swapped) break
        }
        if (!swapped) break
        iteration++
    }
    return closeHull(points)
}

private fun rotateHullToShortestClosure(hull: List<PointCoordinates>): List<PointCoordinates> {
    if (hull.size < 4) return closeHull(hull)
    val open = closeHull(hull).dropLast(1)
    var minIndex = 0
    var minDistance = Double.MAX_VALUE
    for (i in open.indices) {
        val next = (i + 1) % open.size
        val dist = GeoGeometry.distance(open[i], open[next])
        if (dist < minDistance) {
            minDistance = dist
            minIndex = i
        }
    }
    val start = (minIndex + 1) % open.size
    val rotated = mutableListOf<PointCoordinates>()
    for (i in 0 until open.size) {
        rotated.add(open[(start + i) % open.size])
    }
    return closeHull(rotated)
}

private fun attemptConcaveHull(ps: List<PointCoordinates>, k: Int): HullAttempt {

    // the resulting concave hull
    val concaveHull = mutableListOf<MetricPoint>()

    // remove duplicates
    val projectedPoints = project(ps.distinctBy { listOf(it.longitude, it.latitude) })
    val mutablePoints = projectedPoints.toMutableList()

    // k has to be greater than 3 to execute the algorithm
    var kk: Int = max(k, 3)

    // return Points if already Concave Hull
    if (mutablePoints.size < 3) {
        val hull = mutablePoints.map { it.point }
        return HullAttempt(closeHull(hull), true)
    }

    // make sure that k neighbors can be found
    kk = min(kk, mutablePoints.size - 1)

    // find first point and remove from point list
    val firstPoint = findMinYPoint(mutablePoints)
    concaveHull.add(firstPoint)
    var currentPoint = firstPoint
    mutablePoints.remove(firstPoint)
    var previousAngle = 0.0
    var step = 2
    while ((currentPoint !== firstPoint || step == 2) && mutablePoints.size > 0) {

        // after 3 steps add first point to dataset, otherwise hull cannot be closed
        if (step == 5) {
            mutablePoints.add(firstPoint)
        }

        // get k nearest neighbors of current point
        val kNearestPoints = kNearestNeighbors(mutablePoints, currentPoint, kk)

        // sort points by angle clockwise
        val clockwisePoints = sortByAngle(kNearestPoints, currentPoint, previousAngle)

        // check if clockwise angle nearest neighbors are candidates for concave hull
        var its = true
        var i = -1
        while (its && i < clockwisePoints.size - 1) {
            i++
            var lastPoint = 0
            if (clockwisePoints[i] === firstPoint) {
                lastPoint = 1
            }

            // check if possible new concave hull point intersects with others
            var j = 2
            its = false
            while (!its && j < concaveHull.size - lastPoint) {
                its = linesCross(
                    concaveHull[step - 2],
                    clockwisePoints[i],
                    concaveHull[step - 2 - j],
                    concaveHull[step - 1 - j]
                )
                j++
            }
        }

        // if there is no candidate increase k - try again but give up at max k
        if (its) {
            return HullAttempt(emptyList(), false)
        }

        // add candidate to concave hull and remove from dataset
        currentPoint = clockwisePoints[i]

        concaveHull.add(currentPoint)
        mutablePoints.remove(currentPoint)

        // calculate last angle of the concave hull line
        previousAngle = calculateAngle(concaveHull[step - 1], concaveHull[step - 2])
        step++
    }

    // Check if all points are contained in the concave hull
    var insideCheck = true
    var i: Int = mutablePoints.size - 1
    while (insideCheck && i > 0) {
        insideCheck = pointInPolygon(mutablePoints[i], concaveHull)
        i--
    }

    return HullAttempt(closeHull(concaveHull.map { it.point }), insideCheck)
}

/**
 * Note, algorithm has issues with self intersection
 */
fun calculateConcaveHull(ps: List<PointCoordinates>, k: Int, recurseCount: Int = 0, maxRecurse: Int = 10000): List<PointCoordinates> {
    var attempt = recurseCount
    var kk = k
    var lastHull: List<PointCoordinates> = emptyList()
    val convexHull = GeoGeometry.polygonForPoints(ps.toTypedArray())
    val convexArea = GeoGeometry.area(convexHull)
    while (attempt < maxRecurse) {
        val result = attemptConcaveHull(ps, kk)
        val initialHull = if (result.hull.isEmpty()) closeHull(ps) else result.hull
        var cleanedHull = removeSelfIntersections(initialHull, maxRecurse)
        cleanedHull = removeSelfIntersectionsGeo(cleanedHull, maxRecurse)
        cleanedHull = removeSelfIntersections(cleanedHull, maxRecurse)
        lastHull = rotateHullToShortestClosure(cleanedHull)
        if (result.success) {
            val concaveArea =
                if (lastHull.isNotEmpty()) GeoGeometry.area(lastHull.toTypedArray()) else 0.0
            if (concaveArea < convexArea || kk <= 3) {
                return lastHull
            }
            // try to lower k to encourage concavity
            kk = max(3, kk - 1)
        } else {
            kk += 1
        }
        attempt++
    }
    return lastHull.ifEmpty { ps }
}

private fun pointInPolygon(p: MetricPoint, pp: List<MetricPoint>): Boolean {
    var result = false
    var i = 0
    var j: Int = pp.size - 1
    while (i < pp.size) {
        if (pp[i].y > p.y != pp[j].y > p.y &&
            p.x < (pp[j].x - pp[i].x) * (p.y - pp[i].y) /
            (pp[j].y - pp[i].y) + pp[i].x
        ) {
            result = !result
        }
        j = i++
    }
    return result
}
