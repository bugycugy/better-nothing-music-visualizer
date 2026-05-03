package com.better.nothing.music.vizualizer;

import android.content.Context;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Drop-in haptic controller for a visualizer-style amplitude stream.
 * <p>
 * Manifest:
 * <uses-permission android:name="android.permission.VIBRATE" />
 * <p>
 * Notes:
 * - minSdk 14 compatible.
 * - Uses VibratorManager on API 31+.
 * - Keeps a repeating waveform alive and only resubmits when the amplitude changes.
 * - Android 16 envelope APIs exist, but they do not give you a mutable "live motor";
 *   repeating waveform is the practical continuous solution across your API range.
 */
public final class ContinuousHapticEngine {

    private static final String TAG = "ContinuousHapticEngine";

    // Tune this to match your input cadence.
    private static final int HAPTIC_STEP_MS = 16; // ~60 Hz

    // Don't spam the vibrator service faster than this.
    private static final long MIN_RESUBMIT_INTERVAL_MS = 12L;

    // Mapping / shaping
    private static final float DEFAULT_DECAY = 0.85f;
    private static final float DEFAULT_GAMMA = 1.0f;
    private static final float EPSILON = 0.0001f;
    private static final float DEFAULT_SPECTRUM_GAIN = 1.0f;

    // Keep the motor from going completely dead for tiny non-zero values.
    private static final int MIN_ACTIVE_AMPLITUDE = 1;
    private static final int MAX_AMPLITUDE = 255;

    private final Vibrator vibrator;
    @Nullable
    private final VibratorManager vibratorManager;

    // Single repeating waveform: immediate start, then constant "on" segment.
    private final long[] timings = new long[]{0L, HAPTIC_STEP_MS};
    private final int[] amplitudes = new int[]{0, 0};

    private float hapticMultiplier = 1.0f;
    private float hapticGamma = DEFAULT_GAMMA;

    private float decayedState = 0f;
    private float peakTracker = EPSILON;

    private int lastAmplitude = -1;
    private long lastSubmitMs = 0L;
    private boolean waveformActive = false;

    public ContinuousHapticEngine(Context context) {
        Context appContext = Objects.requireNonNull(context, "context").getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            this.vibratorManager = vm;
            this.vibrator = (vm != null) ? vm.getDefaultVibrator() : nullSafeVibrator(appContext);
        } else {
            this.vibratorManager = null;
            this.vibrator = nullSafeVibrator(appContext);
        }
    }

    public synchronized void setHapticMultiplier(float multiplier) {
        this.hapticMultiplier = Math.max(0f, multiplier);
    }

    public synchronized void setHapticGamma(float gamma) {
        this.hapticGamma = Math.max(0.1f, gamma);
    }

    public synchronized boolean isRunning() {
        return waveformActive;
    }

    public synchronized void performHapticFeedback(float rawPeak, @Nullable VisualizerConfig config) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        final float decay = (config != null) ? clamp01(config.decay) : DEFAULT_DECAY;

        // Fast attack, soft release.
        final float current = Math.max(0f, rawPeak) * DEFAULT_SPECTRUM_GAIN * hapticMultiplier;
        if (current > decayedState) {
            decayedState = current;
        } else {
            decayedState = (decay * decayedState) + ((1f - decay) * current);
        }
        if (decayedState < EPSILON) {
            decayedState = 0f;
        }

        peakTracker = Math.max(decayedState, peakTracker * 0.995f);

        final float normalized = (peakTracker > EPSILON)
                ? Math.min(1f, decayedState / peakTracker)
                : 0f;

        final float shaped = (float) Math.pow(normalized, hapticGamma);
        int amplitude = Math.round(shaped * MAX_AMPLITUDE);

        if (amplitude > 0) {
            amplitude = Math.max(MIN_ACTIVE_AMPLITUDE, amplitude);
        }
        amplitude = clampInt(amplitude, 0, MAX_AMPLITUDE);

        if (amplitude <= 0) {
            stopHapticsInternal();
            return;
        }

        final long now = SystemClock.elapsedRealtime();
        if (waveformActive && amplitude == lastAmplitude) {
            return;
        }
        if (waveformActive && (now - lastSubmitMs) < MIN_RESUBMIT_INTERVAL_MS) {
            return;
        }

        submitContinuousWaveform(amplitude);
    }

    public synchronized void stopHaptics() {
        stopHapticsInternal();
        decayedState = 0f;
        peakTracker = EPSILON;
    }

    private void submitContinuousWaveform(int amplitude) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        amplitudes[0] = 0;
        amplitudes[1] = clampInt(amplitude, 0, MAX_AMPLITUDE);

        try {
            VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, 1);
            vibrate(effect);

            waveformActive = true;
            lastAmplitude = amplitude;
            lastSubmitMs = SystemClock.elapsedRealtime();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to submit haptic waveform", e);
            stopHapticsInternal();
        }
    }

    private void vibrate(VibrationEffect effect) {
        if (vibratorManager != null) {
            vibratorManager.vibrate(CombinedVibration.createParallel(effect));
        } else {
            vibrator.vibrate(effect);
        }
    }

    private void stopHapticsInternal() {
        if (!waveformActive) {
            lastAmplitude = -1;
            lastSubmitMs = 0L;
            return;
        }

        try {
            if (vibratorManager != null) {
                vibratorManager.cancel();
            } else if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to stop haptics", e);
        }

        waveformActive = false;
        lastAmplitude = -1;
        lastSubmitMs = 0L;
    }

    private static Vibrator nullSafeVibrator(Context context) {
        VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        return (vm != null) ? vm.getDefaultVibrator() : null;
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}