package com.jillesvangurp.geo

import com.jillesvangurp.geojson.PointCoordinates
import com.jillesvangurp.geojson.latLon
import com.jillesvangurp.geojson.latitude
import com.jillesvangurp.geojson.longitude
import com.jillesvangurp.geojson.normalize
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Optional per-sample measurement tuning profile.
 *
 * Use this when a track receives mixed sensor updates (e.g. GPS + BLE + UWB) and
 * you want to override measurement trust and outlier handling for individual samples.
 */
data class KalmanMeasurementProfile(
    val baseMeasurementNoiseMeters: Double,
    val minMeasurementNoiseMeters: Double,
    val maxMeasurementNoiseMeters: Double,
    val outlierMahalanobisThreshold: Double = 16.0,
) {
    companion object {
        /** Outdoor GPS with faster motion and larger raw error spread. */
        val GPSOutdoorVehicle = KalmanMeasurementProfile(
            baseMeasurementNoiseMeters = 10.0,
            minMeasurementNoiseMeters = 3.0,
            maxMeasurementNoiseMeters = 60.0,
            outlierMahalanobisThreshold = 20.0,
        )

        /** Indoor GPS-like updates with lower motion and moderate jitter. */
        val GPSIndoorSlow = KalmanMeasurementProfile(
            baseMeasurementNoiseMeters = 6.0,
            minMeasurementNoiseMeters = 0.8,
            maxMeasurementNoiseMeters = 35.0,
            outlierMahalanobisThreshold = 16.0,
        )

        /** BLE trilateration / proximity-style positioning with intermittent jumps. */
        val BLE = KalmanMeasurementProfile(
            baseMeasurementNoiseMeters = 5.0,
            minMeasurementNoiseMeters = 1.2,
            maxMeasurementNoiseMeters = 18.0,
            outlierMahalanobisThreshold = 12.0,
        )

        /** UWB ranging with low noise and tight outlier gating. */
        val UWB = KalmanMeasurementProfile(
            baseMeasurementNoiseMeters = 1.0,
            minMeasurementNoiseMeters = 0.2,
            maxMeasurementNoiseMeters = 4.0,
            outlierMahalanobisThreshold = 9.0,
        )
    }

    init {
        require(baseMeasurementNoiseMeters > 0) { "baseMeasurementNoiseMeters must be > 0" }
        require(minMeasurementNoiseMeters > 0) { "minMeasurementNoiseMeters must be > 0" }
        require(maxMeasurementNoiseMeters >= minMeasurementNoiseMeters) {
            "maxMeasurementNoiseMeters must be >= minMeasurementNoiseMeters"
        }
        require(outlierMahalanobisThreshold > 0) { "outlierMahalanobisThreshold must be > 0" }
    }
}

/**
 * Global tracking and filtering configuration for [KalmanGeoTracker].
 *
 * The state model is constant velocity in local tangent plane meters:
 * `x = [east, north, vEast, vNorth]`.
 */
data class KalmanGeoTrackerConfig(
    val timeWindowMillis: Long = 45_000,
    val fastMovementWindowMillis: Long = 10_000,
    val highSpeedThresholdMetersPerSecond: Double = 4.0,
    val substantialMovementMeters: Double = 25.0,
    val processNoisePositionMeters: Double = 1.8,
    val processNoiseSpeedMetersPerSecond: Double = 0.9,
    val baseMeasurementNoiseMeters: Double = 6.0,
    val minMeasurementNoiseMeters: Double = 0.8,
    val maxMeasurementNoiseMeters: Double = 35.0,
    val outlierMahalanobisThreshold: Double = 16.0,
    val innovationVarianceScale: Double = 0.35,
    val initialUncertaintyMeters: Double = 20.0,
    val initialSpeedUncertaintyMetersPerSecond: Double = 4.0,
    val minSpeedForDirectionMetersPerSecond: Double = 0.25,
) {
    companion object {
        /** Preset tuned for fast outdoor vehicle movement with GPS quality updates. */
        val GPSOutdoorVehicle = KalmanGeoTrackerConfig(
            timeWindowMillis = 120_000,
            fastMovementWindowMillis = 20_000,
            highSpeedThresholdMetersPerSecond = 18.0,
            substantialMovementMeters = 150.0,
            processNoisePositionMeters = 4.0,
            processNoiseSpeedMetersPerSecond = 2.2,
            baseMeasurementNoiseMeters = 10.0,
            minMeasurementNoiseMeters = 3.0,
            maxMeasurementNoiseMeters = 60.0,
            outlierMahalanobisThreshold = 20.0,
            innovationVarianceScale = 0.25,
            initialUncertaintyMeters = 40.0,
            initialSpeedUncertaintyMetersPerSecond = 12.0,
            minSpeedForDirectionMetersPerSecond = 1.0,
        )

        /** Preset tuned for indoor, low-speed movement with occasional GPS-like jumps. */
        val GPSIndoorSlow = KalmanGeoTrackerConfig(
            timeWindowMillis = 45_000,
            fastMovementWindowMillis = 10_000,
            highSpeedThresholdMetersPerSecond = 4.0,
            substantialMovementMeters = 25.0,
            processNoisePositionMeters = 1.8,
            processNoiseSpeedMetersPerSecond = 0.9,
            baseMeasurementNoiseMeters = 6.0,
            minMeasurementNoiseMeters = 0.8,
            maxMeasurementNoiseMeters = 35.0,
            outlierMahalanobisThreshold = 16.0,
            innovationVarianceScale = 0.35,
            initialUncertaintyMeters = 20.0,
            initialSpeedUncertaintyMetersPerSecond = 4.0,
            minSpeedForDirectionMetersPerSecond = 0.25,
        )

        /** Preset tuned for BLE-based location streams in indoor environments. */
        val BLE = KalmanGeoTrackerConfig(
            timeWindowMillis = 30_000,
            fastMovementWindowMillis = 8_000,
            highSpeedThresholdMetersPerSecond = 3.0,
            substantialMovementMeters = 18.0,
            processNoisePositionMeters = 2.2,
            processNoiseSpeedMetersPerSecond = 1.0,
            baseMeasurementNoiseMeters = 5.0,
            minMeasurementNoiseMeters = 1.2,
            maxMeasurementNoiseMeters = 18.0,
            outlierMahalanobisThreshold = 12.0,
            innovationVarianceScale = 0.45,
            initialUncertaintyMeters = 15.0,
            initialSpeedUncertaintyMetersPerSecond = 3.0,
            minSpeedForDirectionMetersPerSecond = 0.2,
        )

        /** Preset tuned for low-latency, high-precision UWB positioning. */
        val UWB = KalmanGeoTrackerConfig(
            timeWindowMillis = 20_000,
            fastMovementWindowMillis = 5_000,
            highSpeedThresholdMetersPerSecond = 3.0,
            substantialMovementMeters = 12.0,
            processNoisePositionMeters = 0.9,
            processNoiseSpeedMetersPerSecond = 0.7,
            baseMeasurementNoiseMeters = 1.0,
            minMeasurementNoiseMeters = 0.2,
            maxMeasurementNoiseMeters = 4.0,
            outlierMahalanobisThreshold = 9.0,
            innovationVarianceScale = 0.2,
            initialUncertaintyMeters = 6.0,
            initialSpeedUncertaintyMetersPerSecond = 2.0,
            minSpeedForDirectionMetersPerSecond = 0.1,
        )
    }

    init {
        require(timeWindowMillis > 0) { "timeWindowMillis must be > 0" }
        require(fastMovementWindowMillis in 1..timeWindowMillis) {
            "fastMovementWindowMillis must be > 0 and <= timeWindowMillis"
        }
        require(highSpeedThresholdMetersPerSecond > 0) { "highSpeedThresholdMetersPerSecond must be > 0" }
        require(substantialMovementMeters > 0) { "substantialMovementMeters must be > 0" }
        require(processNoisePositionMeters > 0) { "processNoisePositionMeters must be > 0" }
        require(processNoiseSpeedMetersPerSecond > 0) { "processNoiseSpeedMetersPerSecond must be > 0" }
        require(baseMeasurementNoiseMeters > 0) { "baseMeasurementNoiseMeters must be > 0" }
        require(minMeasurementNoiseMeters > 0) { "minMeasurementNoiseMeters must be > 0" }
        require(maxMeasurementNoiseMeters >= minMeasurementNoiseMeters) {
            "maxMeasurementNoiseMeters must be >= minMeasurementNoiseMeters"
        }
        require(outlierMahalanobisThreshold > 0) { "outlierMahalanobisThreshold must be > 0" }
        require(innovationVarianceScale >= 0) { "innovationVarianceScale must be >= 0" }
        require(initialUncertaintyMeters > 0) { "initialUncertaintyMeters must be > 0" }
        require(initialSpeedUncertaintyMetersPerSecond > 0) {
            "initialSpeedUncertaintyMetersPerSecond must be > 0"
        }
        require(minSpeedForDirectionMetersPerSecond >= 0) {
            "minSpeedForDirectionMetersPerSecond must be >= 0"
        }
    }
}

data class KalmanGeoEstimate(
    /** Timestamp of the estimate in epoch milliseconds. */
    val timestampMillis: Long,
    /** Filtered [longitude, latitude] position. */
    val position: PointCoordinates,
    /** East-west velocity component in meters per second. Positive values move east. */
    val horizontalSpeedMetersPerSecond: Double,
    /** North-south velocity component in meters per second. Positive values move north. */
    val verticalSpeedMetersPerSecond: Double,
    /** Scalar speed from horizontal and vertical velocity components. */
    val speedMetersPerSecond: Double,
    /** Compass bearing in degrees clockwise from north, or null while effectively stationary. */
    val compassDirectionDegrees: Double?,
)

/**
 * One retained input sample and the estimate produced after applying it.
 *
 * Samples are retained for a configurable time window for downstream diagnostics.
 */
data class KalmanGeoSample(
    /** Original input timestamp in epoch milliseconds. */
    val timestampMillis: Long,
    /** Raw observed [longitude, latitude] sample used for this update. */
    val measuredPosition: PointCoordinates,
    /** Optional externally supplied accuracy estimate (1 sigma meters). */
    val measurementAccuracyMeters: Double?,
    /** Filtered estimate after processing this sample. */
    val estimate: KalmanGeoEstimate,
)

/**
 * Multiplatform, dependency-free Kalman tracker for position streams keyed by object id.
 *
 * Characteristics:
 * - 2D constant-velocity state in local tangent meters.
 * - Monotonic timestamp enforcement per tracked id.
 * - Adaptive measurement variance and Mahalanobis outlier rejection.
 * - Configurable retention window with aggressive pruning for high-speed/high-movement tracks.
 */
class KalmanGeoTracker(
    val config: KalmanGeoTrackerConfig = KalmanGeoTrackerConfig(),
) {
    private data class TrackState(
        val origin: PointCoordinates,
        val state: DoubleArray,
        val covariance: Array<DoubleArray>,
        var lastTimestampMillis: Long,
        var aggressivePruneUntilMillis: Long,
        val samples: MutableList<KalmanGeoSample>,
    )

    private val tracks = mutableMapOf<String, TrackState>()

    /**
     * Record a new observation for a tracked object.
     *
     * @param id stable object identifier.
     * @param position observed [longitude, latitude] coordinate.
     * @param timestampMillis monotonically increasing epoch milliseconds for this id.
     * @param measurementAccuracyMeters optional sample-specific 1-sigma accuracy in meters.
     * @param measurementProfile optional sample-specific sensor profile override.
     * @return filtered estimate after processing this observation.
     */
    fun record(
        id: String,
        position: PointCoordinates,
        timestampMillis: Long,
        measurementAccuracyMeters: Double? = null,
        measurementProfile: KalmanMeasurementProfile? = null,
    ): KalmanGeoEstimate {
        require(id.isNotBlank()) { "id must not be blank" }
        require(position.size >= 2) { "position must contain [longitude, latitude]" }
        require(timestampMillis >= 0) { "timestampMillis must be >= 0" }
        measurementAccuracyMeters?.let { require(it > 0) { "measurementAccuracyMeters must be > 0" } }
        val normalizedPosition = position.normalize()

        val track = tracks.getOrPut(id) {
            TrackState(
                origin = latLon(normalizedPosition.latitude, normalizedPosition.longitude),
                state = doubleArrayOf(0.0, 0.0, 0.0, 0.0),
                covariance = diagonal4(
                    config.initialUncertaintyMeters.pow(2),
                    config.initialUncertaintyMeters.pow(2),
                    config.initialSpeedUncertaintyMetersPerSecond.pow(2),
                    config.initialSpeedUncertaintyMetersPerSecond.pow(2)
                ),
                lastTimestampMillis = timestampMillis,
                aggressivePruneUntilMillis = Long.MIN_VALUE,
                samples = mutableListOf(),
            )
        }
        require(timestampMillis >= track.lastTimestampMillis) {
            "timestampMillis must be monotonic per id. id=$id last=${track.lastTimestampMillis} got=$timestampMillis"
        }

        val deltaMillis = timestampMillis - track.lastTimestampMillis
        if (deltaMillis > 0) {
            predict(track, deltaMillis / 1000.0)
            track.lastTimestampMillis = timestampMillis
        } else if (track.samples.isEmpty()) {
            track.lastTimestampMillis = timestampMillis
        }

        val (measurementX, measurementY) = toLocalMeters(track.origin, normalizedPosition)
        update(track, measurementX, measurementY, measurementVariance(measurementAccuracyMeters, measurementProfile), measurementProfile)

        val estimate = estimateAt(track, timestampMillis)
        track.samples.add(
            KalmanGeoSample(
                timestampMillis = timestampMillis,
                measuredPosition = latLon(normalizedPosition.latitude, normalizedPosition.longitude),
                measurementAccuracyMeters = measurementAccuracyMeters,
                estimate = estimate,
            )
        )
        prune(track, timestampMillis)
        return estimate
    }

    /** Most recent estimate for the given id, or null if the id is unknown. */
    fun estimateFor(id: String): KalmanGeoEstimate? = tracks[id]?.samples?.lastOrNull()?.estimate

    /** Number of retained samples for the given id. */
    fun sampleCount(id: String): Int = tracks[id]?.samples?.size ?: 0

    /** Snapshot of retained samples for the given id. */
    fun samples(id: String): List<KalmanGeoSample> = tracks[id]?.samples?.toList() ?: emptyList()

    /** Currently tracked ids. */
    fun trackedIds(): Set<String> = tracks.keys

    /** Remove all state for one id. */
    fun remove(id: String) {
        tracks.remove(id)
    }

    /** Remove all tracked ids and state. */
    fun clear() {
        tracks.clear()
    }

    private fun predict(track: TrackState, dtSeconds: Double) {
        val s = track.state
        s[0] += s[2] * dtSeconds
        s[1] += s[3] * dtSeconds

        val f = arrayOf(
            doubleArrayOf(1.0, 0.0, dtSeconds, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0, dtSeconds),
            doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 1.0),
        )
        val dt = max(1e-3, dtSeconds)
        val dt2 = dt * dt
        val dt3 = dt2 * dt
        val dt4 = dt2 * dt2
        val accelerationStdDev = config.processNoiseSpeedMetersPerSecond
        val accelerationVar = accelerationStdDev * accelerationStdDev
        val randomWalkPositionVar = config.processNoisePositionMeters.pow(2) * dt
        val q = arrayOf(
            doubleArrayOf(dt4 * accelerationVar / 4.0 + randomWalkPositionVar, 0.0, dt3 * accelerationVar / 2.0, 0.0),
            doubleArrayOf(0.0, dt4 * accelerationVar / 4.0 + randomWalkPositionVar, 0.0, dt3 * accelerationVar / 2.0),
            doubleArrayOf(dt3 * accelerationVar / 2.0, 0.0, dt2 * accelerationVar, 0.0),
            doubleArrayOf(0.0, dt3 * accelerationVar / 2.0, 0.0, dt2 * accelerationVar),
        )

        val p = track.covariance
        val predicted = add4(multiply4(multiply4(f, p), transpose4(f)), q)
        copyInto(p, predicted)
    }

    private fun update(
        track: TrackState,
        measurementX: Double,
        measurementY: Double,
        baseMeasurementVariance: Double,
        measurementProfile: KalmanMeasurementProfile?,
    ) {
        val p = track.covariance
        val x = track.state

        var s00 = p[0][0] + baseMeasurementVariance
        val s01 = p[0][1]
        val s10 = p[1][0]
        var s11 = p[1][1] + baseMeasurementVariance
        var det = s00 * s11 - s01 * s10

        if (det < 1e-12) {
            return
        }

        var invS00 = s11 / det
        var invS01 = -s01 / det
        var invS10 = -s10 / det
        var invS11 = s00 / det

        val innovationX = measurementX - x[0]
        val innovationY = measurementY - x[1]
        val d2 = innovationX * (invS00 * innovationX + invS01 * innovationY) +
            innovationY * (invS10 * innovationX + invS11 * innovationY)
        val outlierThreshold = measurementProfile?.outlierMahalanobisThreshold ?: config.outlierMahalanobisThreshold
        if (d2 > outlierThreshold) {
            return
        }

        val adaptiveScale = 1.0 + config.innovationVarianceScale * d2.coerceAtLeast(0.0)
        val measurementVariance = baseMeasurementVariance * adaptiveScale
        if (adaptiveScale != 1.0) {
            s00 = p[0][0] + measurementVariance
            s11 = p[1][1] + measurementVariance
            det = s00 * s11 - s01 * s10
            if (det < 1e-12) {
                return
            }
            invS00 = s11 / det
            invS01 = -s01 / det
            invS10 = -s10 / det
            invS11 = s00 / det
        }

        val k = Array(4) { DoubleArray(2) }
        for (i in 0 until 4) {
            val pi0 = p[i][0]
            val pi1 = p[i][1]
            k[i][0] = pi0 * invS00 + pi1 * invS10
            k[i][1] = pi0 * invS01 + pi1 * invS11
        }

        for (i in 0 until 4) {
            x[i] += k[i][0] * innovationX + k[i][1] * innovationY
        }

        val iKh = arrayOf(
            doubleArrayOf(1.0 - k[0][0], -k[0][1], 0.0, 0.0),
            doubleArrayOf(-k[1][0], 1.0 - k[1][1], 0.0, 0.0),
            doubleArrayOf(-k[2][0], -k[2][1], 1.0, 0.0),
            doubleArrayOf(-k[3][0], -k[3][1], 0.0, 1.0),
        )
        val josephCore = multiply4(multiply4(iKh, p), transpose4(iKh))

        val krkt = Array(4) { i -> DoubleArray(4) { j ->
            measurementVariance * (k[i][0] * k[j][0] + k[i][1] * k[j][1])
        } }
        copyInto(p, add4(josephCore, krkt))
    }

    private fun estimateAt(track: TrackState, timestampMillis: Long): KalmanGeoEstimate {
        val x = track.state
        val horizontal = x[2]
        val vertical = x[3]
        val speed = sqrt(horizontal * horizontal + vertical * vertical)
        val direction = if (speed < config.minSpeedForDirectionMetersPerSecond) {
            null
        } else {
            ((atan2(horizontal, vertical) * GeoGeometry.RADIANS_TO_DEGREES) + 360.0) % 360.0
        }
        val position = toCoordinate(track.origin, x[0], x[1])
        return KalmanGeoEstimate(
            timestampMillis = timestampMillis,
            position = position,
            horizontalSpeedMetersPerSecond = horizontal,
            verticalSpeedMetersPerSecond = vertical,
            speedMetersPerSecond = speed,
            compassDirectionDegrees = direction,
        )
    }

    private fun prune(track: TrackState, nowMillis: Long) {
        val latest = track.samples.lastOrNull() ?: return
        val oldest = track.samples.firstOrNull() ?: return
        val movement = GeoGeometry.distance(oldest.estimate.position, latest.estimate.position)
        val highMovement = movement >= config.substantialMovementMeters
        val highSpeed = latest.estimate.speedMetersPerSecond >= config.highSpeedThresholdMetersPerSecond

        if (highMovement || highSpeed) {
            track.aggressivePruneUntilMillis = nowMillis + config.fastMovementWindowMillis
        }
        val aggressivePrune = nowMillis <= track.aggressivePruneUntilMillis

        val window = if (aggressivePrune) {
            config.fastMovementWindowMillis
        } else {
            config.timeWindowMillis
        }
        val minTimestamp = nowMillis - window
        while (track.samples.size > 1 && track.samples.first().timestampMillis < minTimestamp) {
            track.samples.removeAt(0)
        }
    }

    private fun measurementVariance(measurementAccuracyMeters: Double?, measurementProfile: KalmanMeasurementProfile?): Double {
        val base = measurementProfile?.baseMeasurementNoiseMeters ?: config.baseMeasurementNoiseMeters
        val min = measurementProfile?.minMeasurementNoiseMeters ?: config.minMeasurementNoiseMeters
        val max = measurementProfile?.maxMeasurementNoiseMeters ?: config.maxMeasurementNoiseMeters
        val stddev = (measurementAccuracyMeters ?: base).coerceIn(min, max)
        return stddev * stddev
    }

    private fun toLocalMeters(origin: PointCoordinates, point: PointCoordinates): Pair<Double, Double> {
        val averageLatitudeRadians = ((origin.latitude + point.latitude) / 2.0) * GeoGeometry.DEGREES_TO_RADIANS
        val longitudeDeltaDegrees = wrapLongitudeDelta(point.longitude - origin.longitude)
        val deltaLongitudeRadians = longitudeDeltaDegrees * GeoGeometry.DEGREES_TO_RADIANS
        val deltaLatitudeRadians = (point.latitude - origin.latitude) * GeoGeometry.DEGREES_TO_RADIANS

        val x = deltaLongitudeRadians * GeoGeometry.EARTH_RADIUS_METERS * kotlin.math.cos(averageLatitudeRadians)
        val y = deltaLatitudeRadians * GeoGeometry.EARTH_RADIUS_METERS
        return x to y
    }

    private fun toCoordinate(origin: PointCoordinates, xMeters: Double, yMeters: Double): PointCoordinates {
        return GeoGeometry.translate(origin, xMeters, yMeters).normalize()
    }

    private fun wrapLongitudeDelta(degrees: Double): Double {
        return when {
            degrees > 180.0 -> degrees - 360.0
            degrees < -180.0 -> degrees + 360.0
            else -> degrees
        }
    }

    private fun diagonal4(v0: Double, v1: Double, v2: Double, v3: Double): Array<DoubleArray> {
        return arrayOf(
            doubleArrayOf(v0, 0.0, 0.0, 0.0),
            doubleArrayOf(0.0, v1, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, v2, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, v3),
        )
    }

    private fun multiply4(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val out = Array(4) { DoubleArray(4) }
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                var v = 0.0
                for (k in 0 until 4) {
                    v += a[r][k] * b[k][c]
                }
                out[r][c] = v
            }
        }
        return out
    }

    private fun transpose4(a: Array<DoubleArray>): Array<DoubleArray> {
        val out = Array(4) { DoubleArray(4) }
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                out[r][c] = a[c][r]
            }
        }
        return out
    }

    private fun add4(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val out = Array(4) { DoubleArray(4) }
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                out[r][c] = a[r][c] + b[r][c]
            }
        }
        return out
    }

    private fun copyInto(target: Array<DoubleArray>, source: Array<DoubleArray>) {
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                target[r][c] = source[r][c]
            }
        }
    }
}
