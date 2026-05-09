package com.better.nothing.music.vizualizer

import android.content.Context
import android.os.*
import android.util.Log
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

enum class HapticMode {
    BASS_TO_AMPLITUDE,
    BEAT_DETECTION
}

/**
 * Beat Detection Haptic Engine.
 * Uses live FFT magnitude data and the app's selectable frequency range to trigger
 * a short, precomputed waveform on kick transients.
 */
class BeatDetectionHapticEngine(context: Context) {
    private val TAG = "BeatDetectionHapticEngine"
    private val vibrator: Vibrator?
    private val vibratorManager: VibratorManager?

    private val waveform: VibrationEffect?
    private val deltaHistory = FloatArray(31)
    private val sortedDeltaHistory = FloatArray(deltaHistory.size)
    private var deltaHistoryCount = 0
    private var deltaHistoryIndex = 0
    private var prevEnergy = 0f
    private var lastTriggerMs = 0L

    private val cooldownMs = 120L
    private val energyGate = 0.02f

    init {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
            vibrator = vibratorManager?.defaultVibrator
        } else {
            vibratorManager = null
            vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        waveform = buildKickWaveform()
    }

    fun setHapticMultiplier(multiplier: Float) {
        // This engine does not currently use a multiplier.
    }

    fun resetDetectionState() {
        prevEnergy = 0f
        deltaHistoryCount = 0
        deltaHistoryIndex = 0
        deltaHistory.fill(0f)
        sortedDeltaHistory.fill(0f)
    }

    fun performHapticFeedback(magnitude: FloatArray, hapticRange: AudioProcessor.FrequencyRange?) {
        if (vibrator == null || !vibrator.hasVibrator() || hapticRange == null || magnitude.isEmpty()) {
            return
        }

        val start = max(0, minOf(hapticRange.binLo, magnitude.lastIndex))
        val end = max(start, minOf(hapticRange.binHi, magnitude.lastIndex))

        var sum = 0f
        for (bin in start..end) {
            sum += magnitude[bin]
        }

        performHapticFeedback(sum)
    }

    fun performHapticFeedback(energySum: Float) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }

        val energy = ln(1f + energySum)
        val previousEnergy = prevEnergy
        val delta = energy - previousEnergy

        prevEnergy = energy
        pushDelta(delta)

        val threshold = max(medianDelta() * 2f, 0f)
        val now = SystemClock.elapsedRealtime()
        val cooldownPassed = (now - lastTriggerMs) >= cooldownMs

        if (delta > threshold && previousEnergy < energyGate && cooldownPassed) {
            triggerHaptic()
            lastTriggerMs = now
        }
    }

    private fun pushDelta(delta: Float) {
        deltaHistory[deltaHistoryIndex] = if (delta > 0f) delta else 0f
        deltaHistoryIndex = (deltaHistoryIndex + 1) % deltaHistory.size
        if (deltaHistoryCount < deltaHistory.size) {
            deltaHistoryCount++
        }
    }

    private fun medianDelta(): Float {
        if (deltaHistoryCount == 0) {
            return 0f
        }

        System.arraycopy(deltaHistory, 0, sortedDeltaHistory, 0, deltaHistoryCount)
        for (i in 1 until deltaHistoryCount) {
            val key = sortedDeltaHistory[i]
            var j = i - 1
            while (j >= 0 && sortedDeltaHistory[j] > key) {
                sortedDeltaHistory[j + 1] = sortedDeltaHistory[j]
                j--
            }
            sortedDeltaHistory[j + 1] = key
        }

        return if (deltaHistoryCount % 2 == 1) {
            sortedDeltaHistory[deltaHistoryCount / 2]
        } else {
            val mid = deltaHistoryCount / 2
            (sortedDeltaHistory[mid - 1] + sortedDeltaHistory[mid]) * 0.5f
        }
    }

    private fun triggerHaptic() {
        if (waveform == null) {
            return
        }

        try {
            cancelVibration()
            vibrate(waveform)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to trigger haptic waveform", e)
        }
    }

    private fun buildKickWaveform(): VibrationEffect? {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return null
        }

        val stepMs = 5
        val totalDurationMs = 700
        val stepCount = totalDurationMs / stepMs
        val timings = LongArray(stepCount) { stepMs.toLong() }
        val amplitudes = IntArray(stepCount)

        for (i in 0 until stepCount) {
            val t = i * stepMs.toFloat()
            val normalized = 1f - (t / totalDurationMs)
            val amplitude = if (normalized <= 0f) {
                0f
            } else {
                255f * normalized.pow(4f)
            }
            amplitudes[i] = if (amplitude < 20f) {
                0
            } else {
                amplitude.toInt().coerceIn(0, 255)
            }
        }

        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }

    private fun cancelVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                vibratorManager?.cancel()
            } else {
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel haptics", e)
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
        } else {
            vibrator?.vibrate(effect)
        }
    }

    fun stopHaptics() {
        cancelVibration()
    }
}
