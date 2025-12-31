package com.jillesvangurp.geogeometry

import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.lonLat

data class PolygonPointCloudFixture(
    val name: String,
    val description: String,
    val points: List<PointCoordinates>,
    val k: Int = 5
)

private const val baseLon = 13.4
private const val baseLat = 52.5

private fun fixturePoint(lonOffset: Double, latOffset: Double) = lonLat(baseLon + lonOffset, baseLat + latOffset)

val concaveHullFixtures = listOf(
    PolygonPointCloudFixture(
        name = "C-shape promenade",
        description = "Curved arc with a gap on the east side for a clear concavity",
        points = listOf(
            fixturePoint(-0.018, 0.018),
            fixturePoint(0.0, 0.021),
            fixturePoint(0.018, 0.015),
            fixturePoint(0.022, 0.006),
            fixturePoint(0.012, 0.012),
            fixturePoint(0.008, 0.0),
            fixturePoint(0.012, -0.012),
            fixturePoint(0.022, -0.006),
            fixturePoint(0.018, -0.015),
            fixturePoint(0.0, -0.021),
            fixturePoint(-0.018, -0.018),
            fixturePoint(-0.023, -0.01),
            fixturePoint(-0.024, 0.0),
            fixturePoint(-0.023, 0.01)
        ),
        k = 4
    ),
    PolygonPointCloudFixture(
        name = "U-shape courtyard",
        description = "Open bottom encourages a concave hull while convex hull stays wide",
        points = listOf(
            fixturePoint(-0.02, 0.024),
            fixturePoint(0.02, 0.024),
            fixturePoint(0.022, 0.012),
            fixturePoint(0.022, -0.015),
            fixturePoint(0.01, -0.022),
            fixturePoint(0.0, -0.024),
            fixturePoint(-0.01, -0.022),
            fixturePoint(-0.022, -0.015),
            fixturePoint(-0.022, 0.012),
            fixturePoint(-0.015, 0.018)
        ),
        k = 6
    ),
    PolygonPointCloudFixture(
        name = "Notched rectangle",
        description = "Simple box with a bite on the west edge to keep the hull concave",
        points = listOf(
            fixturePoint(-0.024, -0.024),
            fixturePoint(0.024, -0.024),
            fixturePoint(0.024, 0.024),
            fixturePoint(-0.024, 0.024),
            fixturePoint(-0.024, 0.012),
            fixturePoint(-0.006, 0.008),
            fixturePoint(0.004, -0.002),
            fixturePoint(-0.006, -0.012),
            fixturePoint(-0.024, -0.008)
        ),
        k = 5
    ),
    PolygonPointCloudFixture(
        name = "Sawtooth east edge",
        description = "Zig-zag along one side produces multiple concave pockets",
        points = listOf(
            fixturePoint(-0.02, -0.018),
            fixturePoint(0.018, -0.022),
            fixturePoint(0.024, -0.01),
            fixturePoint(0.018, -0.004),
            fixturePoint(0.024, 0.004),
            fixturePoint(0.018, 0.01),
            fixturePoint(0.024, 0.018),
            fixturePoint(0.012, 0.022),
            fixturePoint(-0.02, 0.018),
            fixturePoint(-0.022, 0.0)
        ),
        k = 6
    ),
    PolygonPointCloudFixture(
        name = "Crescent bay",
        description = "Curved crescent with inner points to force an inward bend",
        points = listOf(
            fixturePoint(-0.018, 0.015),
            fixturePoint(0.0, 0.022),
            fixturePoint(0.018, 0.015),
            fixturePoint(0.024, 0.0),
            fixturePoint(0.018, -0.015),
            fixturePoint(0.0, -0.022),
            fixturePoint(-0.018, -0.015),
            fixturePoint(-0.024, 0.0),
            fixturePoint(-0.02, 0.01),
            fixturePoint(-0.02, -0.01)
        ),
        k = 5
    ),
    PolygonPointCloudFixture(
        name = "Double inlet lake",
        description = "Two shallow inlets on opposite sides create opposing concave edges",
        points = listOf(
            fixturePoint(-0.022, 0.022),
            fixturePoint(0.022, 0.022),
            fixturePoint(0.024, 0.012),
            fixturePoint(0.008, 0.006),
            fixturePoint(0.022, -0.004),
            fixturePoint(0.024, -0.016),
            fixturePoint(-0.024, -0.016),
            fixturePoint(-0.018, -0.004),
            fixturePoint(-0.004, -0.002),
            fixturePoint(-0.018, 0.012)
        ),
        k = 5
    ),
    PolygonPointCloudFixture(
        name = "Corridor with bite",
        description = "Long thin shape with a small bite on the south edge",
        points = listOf(
            fixturePoint(-0.03, 0.012),
            fixturePoint(0.03, 0.012),
            fixturePoint(0.032, 0.0),
            fixturePoint(0.03, -0.012),
            fixturePoint(0.006, -0.016),
            fixturePoint(0.0, -0.008),
            fixturePoint(0.006, 0.0),
            fixturePoint(-0.006, 0.0),
            fixturePoint(-0.03, -0.012),
            fixturePoint(-0.032, 0.0)
        ),
        k = 7
    ),
    PolygonPointCloudFixture(
        name = "Stepped terrace",
        description = "Stair-step outline that produces alternating convex and concave turns",
        points = listOf(
            fixturePoint(-0.022, -0.022),
            fixturePoint(0.0, -0.022),
            fixturePoint(0.012, -0.012),
            fixturePoint(0.022, -0.012),
            fixturePoint(0.022, 0.0),
            fixturePoint(0.012, 0.01),
            fixturePoint(0.012, 0.02),
            fixturePoint(0.0, 0.022),
            fixturePoint(-0.012, 0.014),
            fixturePoint(-0.022, 0.01),
            fixturePoint(-0.022, -0.01)
        ),
        k = 6
    ),
    PolygonPointCloudFixture(
        name = "Arrow with hollow tail",
        description = "Arrow head shape with a hollow tail to exercise thin concave areas",
        points = listOf(
            fixturePoint(-0.02, 0.0),
            fixturePoint(0.0, 0.024),
            fixturePoint(0.02, 0.0),
            fixturePoint(0.008, 0.002),
            fixturePoint(0.008, -0.018),
            fixturePoint(-0.008, -0.018),
            fixturePoint(-0.008, 0.002),
            fixturePoint(-0.015, 0.01)
        ),
        k = 5
    ),
    PolygonPointCloudFixture(
        name = "Serpentine ridge",
        description = "Gentle S-curve boundary that should remain simple yet concave",
        points = listOf(
            fixturePoint(-0.024, 0.018),
            fixturePoint(-0.012, 0.02),
            fixturePoint(0.0, 0.018),
            fixturePoint(0.012, 0.014),
            fixturePoint(0.022, 0.016),
            fixturePoint(0.024, 0.006),
            fixturePoint(0.012, -0.004),
            fixturePoint(0.0, -0.002),
            fixturePoint(-0.012, -0.012),
            fixturePoint(-0.024, -0.014),
            fixturePoint(-0.02, -0.004)
        ),
        k = 6
    )
)
