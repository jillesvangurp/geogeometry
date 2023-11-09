package com.jillesvangurp.geo

import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import kotlin.math.*

/**
 * Adapted from MIT licensed https://github.com/Merowech/java-concave-hull
 * by @author Udo Schlegel - Udo.3.Schlegel(at)uni-konstanz.de
 *
 * Code has been converted to Kotlin and refactored a little.
 *
 * Note. It currently  has issues with self intersecting. So, I don't recommend using
 * this in its current form.
 */

//private fun euclideanDistance(a: PointCoordinates, b: PointCoordinates): Double {
//    return (a.longitude - b.longitude).pow(2.0) + (a.latitude - b.latitude).pow(2.0)
//}

//private fun kNearestNeighbors(
//    l: List<PointCoordinates>,
//    q: PointCoordinates,
//    k: Int
//): List<PointCoordinates> {
//    val nearestList = mutableListOf<Pair<Double, PointCoordinates>>()
//    for (o in l) {
//        nearestList.add(Pair(euclideanDistance(q, o), o))
//    }
//    nearestList.sortBy { it.first }
//
//    val result = mutableListOf<PointCoordinates>()
//    for (i in 0 until min(k, nearestList.size)) {
//        result.add(nearestList[i].second)
//    }
//    return result
//}

private fun kNearestNeighbors(l: List<PointCoordinates>, q: PointCoordinates, k: Int): List<PointCoordinates> {
    return l.map { o -> Pair(GeoGeometry.distance(q, o), o) }
        .sortedBy { it.first }
        .take(k)
        .map { it.second }
}


private fun findMinYPoint(l: List<PointCoordinates>): PointCoordinates = l.minByOrNull { it.latitude } ?: error("list should not be empty")

//private fun calculateAngle(o1: PointCoordinates, o2: PointCoordinates) = atan2(o2.latitude - o1.latitude, o2.longitude - o1.longitude)

private fun calculateAngle(o1: PointCoordinates, o2: PointCoordinates): Double {
    val angle = atan2(o2.latitude - o1.latitude, o2.longitude - o1.longitude)
    return (angle + 2 * PI) % (2 * PI)
}

//private fun angleDifference(a1: Double, a2: Double): Double {
//    // calculate angle difference in clockwise directions as radians
//    return when {
//        a1 > 0 && a2 >= 0 && a1 > a2 -> {
//            abs(a1 - a2)
//        }
//        a1 >= 0 && a2 > 0 && a1 < a2 -> {
//            2 * PI + a1 - a2
//        }
//        a1 < 0 && a2 <= 0 && a1 < a2 -> {
//            2 * PI + a1 + abs(a2)
//        }
//        a1 <= 0 && a2 < 0 && a1 > a2 -> {
//            abs(a1 - a2)
//        }
//        a1 <= 0 && 0 < a2 -> {
//            2 * PI + a1 - a2
//        }
//        a1 >= 0 && 0 >= a2 -> {
//            a1 + abs(a2)
//        }
//        else -> {
//            0.0
//        }
//    }
//}

private fun angleDifference(a1: Double, a2: Double): Double {
    val diff = (a2 - a1 + 2 * PI) % (2 * PI)
    return if (diff > PI) 2 * PI - diff else diff
}

private fun sortByAngle(
    l: List<PointCoordinates>,
    q: PointCoordinates,
    a: Double
): List<PointCoordinates> {
    // Sort by angle descending
    return l.sortedBy {
        angleDifference(a, calculateAngle(q, it))
    }
}

/**
 * Note, algorithm has issues with self intersection
 */
tailrec fun calculateConcaveHull(ps: List<PointCoordinates>, k: Int, recurseCount: Int = 0, maxRecurse: Int = 10000): List<PointCoordinates> {

    // the resulting concave hull
    val concaveHull = mutableListOf<PointCoordinates>()

    // remove duplicates
    val mutablePoints = ps.distinct().toMutableList()

    // k has to be greater than 3 to execute the algorithm
    var kk: Int = max(k, 3)

    // return Points if already Concave Hull
    if (mutablePoints.size < 3) {
        return mutablePoints
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
                its = GeoGeometry.linesCross(
                    concaveHull[step - 2],
                    clockwisePoints[i],
                    concaveHull[step - 2 - j],
                    concaveHull[step - 1 - j]
                )
                j++
            }
        }

        // if there is no candidate increase k - try again but give up at max k
        if (its && recurseCount < maxRecurse) {
            return calculateConcaveHull(ps, k + 1, recurseCount+1)
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
//        insideCheck = pointInPolygon(mutablePoints[i], concaveHull)
        insideCheck = GeoGeometry.polygonContains(mutablePoints[i], concaveHull.toTypedArray())
        i--
    }

    // if not all points inside -  try again
    return if (!insideCheck && recurseCount< maxRecurse) {
        calculateConcaveHull(ps, k + 1, recurseCount+1)
    } else {
         concaveHull
    }
}

private fun pointInPolygon(p: PointCoordinates, pp: List<PointCoordinates>): Boolean {
    var result = false
    var i = 0
    var j: Int = pp.size - 1
    while (i < pp.size) {
        if (pp[i].latitude > p.latitude != pp[j].latitude > p.latitude &&
            p.longitude < (pp[j].longitude - pp[i].longitude) * (p.latitude - pp[i].latitude) /
            (pp[j].latitude - pp[i].latitude) + pp[i].longitude
        ) {
            result = !result
        }
        j = i++
    }
    return result
}

