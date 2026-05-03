package com.better.nothing.music.vizualizer;

import java.util.Arrays;

/**
 * Handles glyph state computation, breathing effects, and frame rendering.
 */
public class GlyphRenderer {

    private static final float PEAK_FALLOFF = 0.9995f;
    private static final float SPECTRUM_GAIN = 4f;
    private static final float EPSILON = 0.000001f;
    private static final float SILENCE_THRESHOLD = 0.005f;
    private static final long BREATH_DELAY_MS = 3000L;
    private static final long BREATH_PERIOD_MS = 5000L;
    private static final long FLASH_DURATION_MS = 200L;

    private final float mGamma;
    private final boolean mIdleBreathingEnabled;
    private final boolean mNotificationFlashEnabled;

    private float[] mCurrentLightState = new float[0];
    private float[] mZonePeaks = new float[0];
    private float[] mDecayedFrequencyState = new float[0];
    private int mLastHash = Integer.MIN_VALUE;
    private long mLastSendMs = 0L;
    private long mSilenceStartTimeMs = 0;
    private long mLastNotificationFlashMs = 0;

    public void setIdleBreathingEnabled(boolean enabled) {
        mIdleBreathingEnabled = enabled;
        if (!enabled) {
            mSilenceStartTimeMs = 0;
        }
    }

    public void resetState(AudioProcessor.VisualizerConfig config) {
        if (config == null) {
            mCurrentLightState = new float[0];
            mZonePeaks = new float[0];
            mDecayedFrequencyState = new float[0];
        } else {
            mCurrentLightState = new float[config.zones.length];
            mZonePeaks = new float[config.zones.length];
            Arrays.fill(mZonePeaks, EPSILON);
            mDecayedFrequencyState = new float[config.uniqueRanges.length];
        }
        mLastHash = Integer.MIN_VALUE;
        mLastSendMs = 0L;
        mSilenceStartTimeMs = 0;
    }

    public int[] processFrame(float[] uniquePeaks, AudioProcessor.VisualizerConfig config, long nowMs) {
        if (config == null || config.zones.length == 0) {
            return new int[0];
        }

        ensureStateArrays(config.zones.length, config.uniqueRanges.length);

        float[] nextLightState = computeNextLightState(uniquePeaks, config);

        if (nowMs - mLastNotificationFlashMs < FLASH_DURATION_MS) {
            Arrays.fill(nextLightState, 1.0f);
        } else if (mIdleBreathingEnabled) {
            applyIdleBreathing(nextLightState, uniquePeaks, nowMs);
        }

        System.arraycopy(nextLightState, 0, mCurrentLightState, 0, nextLightState.length);

        int[] frameColors = buildFrameColors(nextLightState, config.zones.length);
        int frameHash = Arrays.hashCode(frameColors);
        if (frameHash == mLastHash) {
            return null; // No change
        }

        mLastHash = frameHash;
        mLastSendMs = nowMs;
        return frameColors;
    }

    public void triggerNotificationFlash(long nowMs) {
        if (mNotificationFlashEnabled) {
            mLastNotificationFlashMs = nowMs;
        }
    }

    public float[] getCurrentLightState() {
        return mCurrentLightState != null ? Arrays.copyOf(mCurrentLightState, mCurrentLightState.length) : new float[0];
    }

    private float[] computeNextLightState(float[] uniquePeaks, AudioProcessor.VisualizerConfig config) {
        float[] decayedFrequencyState = computeDecayedFrequencyState(uniquePeaks, config);
        float[] nextState = new float[config.zones.length];

        for (int zoneIndex = 0; zoneIndex < config.zones.length; zoneIndex++) {
            float rawZonePeak = 0f;
            int[] overlappingRanges = config.zoneToRangeIndices[zoneIndex];
            for (int rangeIndex : overlappingRanges) {
                if (rangeIndex >= 0 && rangeIndex < decayedFrequencyState.length) {
                    rawZonePeak = Math.max(rawZonePeak, decayedFrequencyState[rangeIndex]);
                }
            }

            mZonePeaks[zoneIndex] = Math.max(rawZonePeak, mZonePeaks[zoneIndex] * PEAK_FALLOFF);
            if (mZonePeaks[zoneIndex] < EPSILON) {
                mZonePeaks[zoneIndex] = EPSILON;
            }

            float normalized = rawZonePeak / mZonePeaks[zoneIndex];
            float shaped = normalized * normalized;
            float mapped = applyPercentSlice(shaped, config.zones[zoneIndex]);
            nextState[zoneIndex] = mapped < EPSILON ? 0f : mapped;
        }

        return nextState;
    }

    private float[] computeDecayedFrequencyState(float[] uniquePeaks, AudioProcessor.VisualizerConfig config) {
        float[] next = new float[mDecayedFrequencyState.length];
        for (int i = 0; i < next.length; i++) {
            float current = (i < uniquePeaks.length ? uniquePeaks[i] : 0f) * SPECTRUM_GAIN;
            float risen = Math.max(mDecayedFrequencyState[i], current);
            float decayed = (config.decay * risen) + ((1f - config.decay) * current);
            next[i] = decayed < EPSILON ? 0f : decayed;
        }
        System.arraycopy(next, 0, mDecayedFrequencyState, 0, next.length);
        return next;
    }

    private void applyIdleBreathing(float[] nextState, float[] uniquePeaks, long nowMs) {
        boolean isSilent = true;
        for (float peak : uniquePeaks) {
            if (peak * SPECTRUM_GAIN > SILENCE_THRESHOLD) {
                isSilent = false;
                break;
            }
        }

        if (isSilent) {
            if (mSilenceStartTimeMs == 0) {
                mSilenceStartTimeMs = nowMs;
            }

            long silenceDuration = nowMs - mSilenceStartTimeMs;
            if (silenceDuration > BREATH_DELAY_MS) {
                float progress = ((float)((silenceDuration - BREATH_DELAY_MS) % BREATH_PERIOD_MS)) / BREATH_PERIOD_MS;
                float breathIntensity = (float) (0.5f * (1.0f + Math.sin(2.0 * Math.PI * progress - Math.PI / 2.0)));
                float subtleBreath = breathIntensity * 0.15f;

                for (int i = 0; i < nextState.length; i++) {
                    nextState[i] = Math.max(nextState[i], subtleBreath);
                }
            }
        } else {
            mSilenceStartTimeMs = 0;
        }
    }

    private int[] buildFrameColors(float[] normalizedLightState, int expectedLength) {
        int[] frameColors = new int[expectedLength];
        int count = Math.min(normalizedLightState.length, expectedLength);
        for (int i = 0; i < count; i++) {
            frameColors[i] = Math.round(applyGamma(normalizedLightState[i]) * 4095f);
        }
        return frameColors;
    }

    private float applyGamma(float normalizedValue) {
        if (normalizedValue <= 0f) {
            return 0f;
        }
        return (float) Math.pow(normalizedValue, mGamma);
    }

    private void ensureStateArrays(int zoneCount, int uniqueRangeCount) {
        if (mCurrentLightState.length == zoneCount
                && mZonePeaks.length == zoneCount
                && mDecayedFrequencyState.length == uniqueRangeCount) {
            return;
        }

        mCurrentLightState = new float[zoneCount];
        mZonePeaks = new float[zoneCount];
        Arrays.fill(mZonePeaks, EPSILON);
        mDecayedFrequencyState = new float[uniqueRangeCount];
        mLastHash = Integer.MIN_VALUE;
    }

    private static float applyPercentSlice(float normalizedValue, AudioProcessor.ZoneSpec zone) {
        if (!zone.hasPercentSlice()) {
            return normalizedValue;
        }

        float low = Math.min(zone.lowPercent, zone.highPercent);
        float high = Math.max(zone.lowPercent, zone.highPercent);
        float percent = normalizedValue * 100f;

        if (percent <= low) {
            return 0f;
        }
        if (percent >= high || high == low) {
            return 1f;
        }
        return (percent - low) / (high - low);
    }
}