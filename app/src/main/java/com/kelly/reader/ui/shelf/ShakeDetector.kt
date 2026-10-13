package com.kelly.reader.ui.shelf

// about 1.5 g: a firm shake passes it, a phone carried while walking rarely does
const val STRONG_ACCELERATION = 15f
const val SHAKE_DURATION_MS = 3_000L

// a shaking hand peaks several times a second, so a longer quiet spell means it stopped
const val MAX_SHAKE_GAP_MS = 500L

private const val NOT_SHAKING = -1L

/**
 * Tells a long shake apart from ordinary movement. It is fed accelerometer readings and answers
 * true once the phone has been shaken for [durationMs] without a pause longer than [maxGapMs].
 * A reading counts as part of a shake when the acceleration, gravity included, is over
 * [strongAcceleration] in m/s².
 */
class ShakeDetector(
    private val durationMs: Long = SHAKE_DURATION_MS,
    private val maxGapMs: Long = MAX_SHAKE_GAP_MS,
    private val strongAcceleration: Float = STRONG_ACCELERATION,
) {

    private var startMs = NOT_SHAKING
    private var lastStrongMs = NOT_SHAKING

    /**
     * Takes one reading in m/s² taken at [timeMs] and returns true when it completes a long
     * shake. The count then starts over, so a shake that keeps going fires again only after
     * another full [durationMs].
     */
    fun onReading(x: Float, y: Float, z: Float, timeMs: Long): Boolean {
        if (startMs != NOT_SHAKING && timeMs - lastStrongMs > maxGapMs) {
            startMs = NOT_SHAKING
        }
        val strong = x * x + y * y + z * z > strongAcceleration * strongAcceleration
        if (!strong) return false
        lastStrongMs = timeMs
        if (startMs == NOT_SHAKING) startMs = timeMs
        if (timeMs - startMs < durationMs) return false
        startMs = NOT_SHAKING
        return true
    }
}
