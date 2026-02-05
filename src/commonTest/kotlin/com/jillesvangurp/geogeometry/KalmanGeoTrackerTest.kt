package com.jillesvangurp.geogeometry

import com.jillesvangurp.geo.GeoGeometry
import com.jillesvangurp.geo.KalmanGeoTracker
import com.jillesvangurp.geo.KalmanGeoTrackerConfig
import com.jillesvangurp.geojson.latLon
import com.jillesvangurp.geojson.longitude
import com.jillesvangurp.geojson.normalize
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFailsWith

class KalmanGeoTrackerTest {
    @Test
    fun shouldExposeSensibleTechnologyPresets() {
        val gpsVehicle = KalmanGeoTrackerConfig.GPSOutdoorVehicle
        val gpsIndoor = KalmanGeoTrackerConfig.GPSIndoorSlow
        val ble = KalmanGeoTrackerConfig.BLE
        val uwb = KalmanGeoTrackerConfig.UWB

        gpsVehicle.highSpeedThresholdMetersPerSecond shouldBe 18.0
        gpsIndoor.highSpeedThresholdMetersPerSecond shouldBe 4.0
        ble.baseMeasurementNoiseMeters shouldBe 5.0
        uwb.baseMeasurementNoiseMeters shouldBe 1.0

        (gpsVehicle.baseMeasurementNoiseMeters > ble.baseMeasurementNoiseMeters) shouldBe true
        (ble.baseMeasurementNoiseMeters > uwb.baseMeasurementNoiseMeters) shouldBe true
        (gpsVehicle.timeWindowMillis > uwb.timeWindowMillis) shouldBe true
    }

    @Test
    fun shouldTrackPositionSpeedAndDirection() {
        val tracker = KalmanGeoTracker(
            KalmanGeoTrackerConfig(
                timeWindowMillis = 120_000,
                fastMovementWindowMillis = 20_000,
                baseMeasurementNoiseMeters = 8.0,
                processNoisePositionMeters = 1.5,
                processNoiseSpeedMetersPerSecond = 0.8,
            )
        )
        val id = "asset-1"
        val start = latLon(52.0, 13.0)

        val expectedHorizontal = 2.0
        val expectedVertical = 0.5
        val expectedSpeed = kotlin.math.sqrt(expectedHorizontal * expectedHorizontal + expectedVertical * expectedVertical)
        val expectedHeading = GeoGeometry.headingFromTwoPoints(
            start,
            GeoGeometry.translate(start, expectedHorizontal, expectedVertical)
        )

        val noisePattern = listOf(-2.0, 1.0, -1.0, 0.5, 1.5, -0.5, 0.0, 0.8)
        var lastEstimate = tracker.record(id, start, timestampMillis = 0L, measurementAccuracyMeters = 8.0)
        for (i in 1..35) {
            val ideal = GeoGeometry.translate(start, expectedHorizontal * i, expectedVertical * i)
            val noisy = GeoGeometry.translate(
                ideal,
                noisePattern[i % noisePattern.size],
                noisePattern[(i + 3) % noisePattern.size]
            )
            lastEstimate = tracker.record(id, noisy, timestampMillis = i * 1000L, measurementAccuracyMeters = 8.0)
        }

        val finalTruth = GeoGeometry.translate(start, expectedHorizontal * 35, expectedVertical * 35)
        GeoGeometry.distance(lastEstimate.position, finalTruth) shouldBeLessThan 12.0
        abs(lastEstimate.horizontalSpeedMetersPerSecond - expectedHorizontal) shouldBeLessThan 0.9
        abs(lastEstimate.verticalSpeedMetersPerSecond - expectedVertical) shouldBeLessThan 0.7
        abs(lastEstimate.speedMetersPerSecond - expectedSpeed) shouldBeLessThan 1.0
        val heading = lastEstimate.compassDirectionDegrees.shouldNotBeNull()
        abs(heading - expectedHeading) shouldBeLessThan 30.0
    }

    @Test
    fun shouldKeepOnlyConfiguredTimeWindowWhenMovementIsNormal() {
        val tracker = KalmanGeoTracker(
            KalmanGeoTrackerConfig(
                timeWindowMillis = 10_000,
                fastMovementWindowMillis = 4_000,
                highSpeedThresholdMetersPerSecond = 40.0,
                substantialMovementMeters = 1_000.0,
                baseMeasurementNoiseMeters = 3.0,
            )
        )
        val id = "asset-2"
        val start = latLon(52.0, 13.0)

        for (i in 0..25) {
            val point = GeoGeometry.translate(start, i.toDouble(), 0.0)
            tracker.record(id, point, timestampMillis = i * 1000L, measurementAccuracyMeters = 3.0)
        }

        tracker.sampleCount(id) shouldBeLessThanOrEqual 11
        val kept = tracker.samples(id)
        kept.first().timestampMillis shouldBe 15_000L
        kept.last().timestampMillis shouldBe 25_000L
    }

    @Test
    fun shouldDiscardOlderLocationsWhenSpeedIsHigh() {
        val tracker = KalmanGeoTracker(
            KalmanGeoTrackerConfig(
                timeWindowMillis = 60_000,
                fastMovementWindowMillis = 5_000,
                highSpeedThresholdMetersPerSecond = 3.0,
                substantialMovementMeters = 500.0,
                processNoisePositionMeters = 1.0,
                processNoiseSpeedMetersPerSecond = 0.6,
                baseMeasurementNoiseMeters = 2.0,
            )
        )
        val id = "asset-3"
        val start = latLon(52.0, 13.0)

        for (i in 0..15) {
            val point = GeoGeometry.translate(start, i * 10.0, 0.0)
            tracker.record(id, point, timestampMillis = i * 1000L, measurementAccuracyMeters = 2.0)
        }

        val kept = tracker.samples(id)
        kept.size shouldBeLessThanOrEqual 6
        kept.last().timestampMillis shouldBe 15_000L
        kept.first().timestampMillis shouldBe 10_000L
    }

    @Test
    fun shouldDiscardOlderLocationsWhenMovementIsSubstantial() {
        val tracker = KalmanGeoTracker(
            KalmanGeoTrackerConfig(
                timeWindowMillis = 60_000,
                fastMovementWindowMillis = 5_000,
                highSpeedThresholdMetersPerSecond = 100.0,
                substantialMovementMeters = 20.0,
                baseMeasurementNoiseMeters = 2.0,
            )
        )
        val id = "asset-4"
        val start = latLon(52.0, 13.0)

        for (i in 0..20) {
            val point = GeoGeometry.translate(start, i * 2.0, 0.0)
            tracker.record(id, point, timestampMillis = i * 1000L, measurementAccuracyMeters = 2.0)
        }

        val kept = tracker.samples(id)
        kept.size shouldBeLessThanOrEqual 12
        kept.last().timestampMillis shouldBe 20_000L
        ((20_000L - kept.first().timestampMillis) <= 10_000L) shouldBe true
    }

    @Test
    fun shouldRejectOutOfOrderTimestamps() {
        val tracker = KalmanGeoTracker()
        val id = "asset-ordered"
        val point = latLon(52.0, 13.0)
        tracker.record(id, point, timestampMillis = 2_000L)
        assertFailsWith<IllegalArgumentException> {
            tracker.record(id, point, timestampMillis = 1_500L)
        }
    }

    @Test
    fun shouldHandleVariableUpdateIntervals() {
        val tracker = KalmanGeoTracker(
            KalmanGeoTrackerConfig(
                processNoisePositionMeters = 1.2,
                processNoiseSpeedMetersPerSecond = 0.8,
                baseMeasurementNoiseMeters = 2.0,
            )
        )
        val id = "asset-variable-dt"
        val start = latLon(52.0, 13.0)
        val vx = 1.5
        val vy = 0.2
        val timestamps = listOf(0L, 100L, 300L, 1_300L, 5_300L, 5_400L, 10_000L, 14_000L)

        var lastEstimate = tracker.record(id, start, timestamps.first(), measurementAccuracyMeters = 2.0)
        for (i in 1 until timestamps.size) {
            val t = timestamps[i]
            val dt = t / 1000.0
            val truth = GeoGeometry.translate(start, vx * dt, vy * dt)
            val xNoise = (i % 3 - 1) * 0.4
            val yNoise = if (i % 2 == 0) 0.25 else -0.25
            val noisy = GeoGeometry.translate(truth, xNoise, yNoise)
            lastEstimate = tracker.record(id, noisy, t, measurementAccuracyMeters = 2.0)
        }

        val finalTruth = GeoGeometry.translate(start, vx * 14.0, vy * 14.0)
        GeoGeometry.distance(lastEstimate.position, finalTruth) shouldBeLessThan 8.0
        abs(lastEstimate.horizontalSpeedMetersPerSecond - vx) shouldBeLessThan 1.0
        abs(lastEstimate.verticalSpeedMetersPerSecond - vy) shouldBeLessThan 0.8
    }

    @Test
    fun shouldHandleAntimeridianCrossing() {
        val tracker = KalmanGeoTracker(
            KalmanGeoTrackerConfig(
                processNoisePositionMeters = 1.0,
                processNoiseSpeedMetersPerSecond = 0.7,
                baseMeasurementNoiseMeters = 1.5,
            )
        )
        val id = "asset-dateline"
        val start = latLon(0.0, 179.9997)
        val vx = 6.0

        var lastEstimate = tracker.record(id, start, 0L, measurementAccuracyMeters = 1.5)
        for (i in 1..20) {
            val truth = GeoGeometry.translate(start, vx * i, 0.0).normalize()
            val noisy = GeoGeometry.translate(truth, if (i % 2 == 0) 0.3 else -0.3, 0.1).normalize()
            lastEstimate = tracker.record(id, noisy, i * 1000L, measurementAccuracyMeters = 1.5)
        }

        val finalTruth = GeoGeometry.translate(start, vx * 20, 0.0).normalize()
        GeoGeometry.distance(lastEstimate.position, finalTruth) shouldBeLessThan 12.0
        (lastEstimate.position.longitude in -180.0..180.0) shouldBe true
    }

    @Test
    fun shouldHandleHighLatitudeMotion() {
        val tracker = KalmanGeoTracker(
            KalmanGeoTrackerConfig(
                processNoisePositionMeters = 0.9,
                processNoiseSpeedMetersPerSecond = 0.6,
                baseMeasurementNoiseMeters = 1.2,
            )
        )
        val id = "asset-high-lat"
        val start = latLon(85.0, 10.0)
        val vx = 1.8
        val vy = 0.4

        var lastEstimate = tracker.record(id, start, 0L, measurementAccuracyMeters = 1.2)
        for (i in 1..30) {
            val truth = GeoGeometry.translate(start, vx * i, vy * i)
            val noisy = GeoGeometry.translate(truth, if (i % 2 == 0) 0.25 else -0.25, if (i % 3 == 0) 0.3 else -0.1)
            lastEstimate = tracker.record(id, noisy, i * 1000L, measurementAccuracyMeters = 1.2)
        }

        val finalTruth = GeoGeometry.translate(start, vx * 30, vy * 30)
        GeoGeometry.distance(lastEstimate.position, finalTruth) shouldBeLessThan 10.0
        abs(lastEstimate.horizontalSpeedMetersPerSecond - vx) shouldBeLessThan 0.9
    }
}
